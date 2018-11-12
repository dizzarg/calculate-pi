package ru.dkadyrov.calculate.pi.distributed.master;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import ru.dkadyrov.calculate.pi.distributed.common.ChildrenCache;
import ru.dkadyrov.calculate.pi.distributed.common.ServerIdGenerator;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClient;

import java.util.List;
import java.util.Random;

@Slf4j
public class Master {

    private ZKClient zk;
    private Random random = new Random(this.hashCode());
    private String serverId = ServerIdGenerator.nextId();
    private MasterStates state = MasterStates.RUNNING;
    private ChildrenCache tasksCache;
    private ChildrenCache workersCache;

    public Master(ZKClient zkClient) {
        this.zk = zkClient;
        createIfAbsent("/workers", new byte[0]);
        createIfAbsent("/assign", new byte[0]);
        createIfAbsent("/tasks", new byte[0]);
        createIfAbsent("/status", new byte[0]);
    }

    void createIfAbsent(String path, byte[] data) {
        zk.create(path,
                data,
                CreateMode.PERSISTENT,
                (rc, p, ctx, name) -> {
                    KeeperException.Code code = KeeperException.Code.get(rc);
                    switch (code) {
                        case CONNECTIONLOSS:
                            createIfAbsent(p, (byte[]) ctx);
                            break;
                        case OK:
                            log.info("Parent created");
                            break;
                        case NODEEXISTS:
                            log.warn("Parent already registered: {}", p);
                            break;
                        default:
                            log.error("Something went wrong: ", KeeperException.create(code, p));
                    }
                },
                data);
    }

    public void runForMaster() {
        log.info("Running for master");
        zk.create("/master",
                serverId.getBytes(),
                CreateMode.EPHEMERAL,
                (rc, path, ctx, name) -> {
                    switch (KeeperException.Code.get(rc)) {
                        case CONNECTIONLOSS:
                            checkMaster();
                            break;
                        case OK:
                            state = MasterStates.ELECTED;
                            takeLeadership();
                            break;
                        case NODEEXISTS:
                            state = MasterStates.NOTELECTED;
                            masterExists();
                            break;
                        default:
                            state = MasterStates.NOTELECTED;
                            log.error("Something went wrong when running for master.",
                                    KeeperException.create(KeeperException.Code.get(rc), path));
                    }
                    log.info("I'm " + (state == MasterStates.ELECTED ? "" : "not ") + "the leader " + serverId);
                });
    }

    private void checkMaster() {
        zk.getData(
                "/master",
                new AsyncCallback.DataCallback() {
                    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
                        switch (KeeperException.Code.get(rc)) {
                            case CONNECTIONLOSS:
                                checkMaster();
                                break;
                            case NONODE:
                                runForMaster();
                                break;
                            case OK:
                                if (serverId.equals(new String(data))) {
                                    state = MasterStates.ELECTED;
                                    takeLeadership();
                                } else {
                                    state = MasterStates.NOTELECTED;
                                    masterExists();
                                }
                                break;
                            default:
                                log.error("Error when reading data.",
                                        KeeperException.create(KeeperException.Code.get(rc), path));
                        }
                    }
                }
        );
    }

    private void masterExists() {
        zk.exists("/master", masterExistsWatcher, masterExistsCallback);
    }

    private void takeLeadership() {
        log.info("Going for list of workers");
        getWorkers();
        RecoveredAssignments.recover(zk, (rc, tasks) -> {
            if (rc == RecoveredAssignments.RecoveryCallback.FAILED) {
                log.error("Recovery of assigned tasks failed.");
            } else {
                log.info("Assigning recovered tasks");
                getTasks();
            }
        });
    }

    private void getTasks() {
        zk.getChildren("/tasks",
                tasksChangeWatcher,
                tasksGetChildrenCallback);
    }

    private void getWorkers() {
        zk.getChildren("/workers",
                workersChangeWatcher,
                workersGetChildrenCallback);
    }

    Watcher workersChangeWatcher = new Watcher() {
        public void process(WatchedEvent e) {
            if (e.getType() == Event.EventType.NodeChildrenChanged) {
                getWorkers();
            }
        }
    };

    AsyncCallback.ChildrenCallback workersGetChildrenCallback = new AsyncCallback.ChildrenCallback() {
        public void processResult(int rc, String path, Object ctx, List<String> children) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    getWorkers();
                    break;
                case OK:
                    log.info("Succesfully got a list of workers: "
                            + children.size()
                            + " workers");
                    reassignAndSet(children);
                    break;
                default:
                    log.error("getChildren failed",
                            KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    Watcher tasksChangeWatcher = new Watcher() {
        public void process(WatchedEvent e) {
            if (e.getType() == Event.EventType.NodeChildrenChanged) {
                getTasks();
            }
        }
    };

    AsyncCallback.ChildrenCallback tasksGetChildrenCallback = new AsyncCallback.ChildrenCallback() {
        public void processResult(int rc, String path, Object ctx, List<String> children) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    getTasks();
                    break;
                case OK:
                    List<String> toProcess;
                    if (tasksCache == null) {
                        tasksCache = new ChildrenCache(children);
                        toProcess = children;
                    } else {
                        toProcess = tasksCache.addedAndSet(children);
                    }
                    if (toProcess != null) {
                        assignTasks(toProcess);
                    }
                    break;
                default:
                    log.error("getChildren failed. {}",
                            KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    private void assignTasks(List<String> tasks) {
        for (String task : tasks) {
            getTaskData(task);
        }
    }

    private void reassignAndSet(List<String> children) {
        List<String> toProcess;

        if (workersCache == null) {
            workersCache = new ChildrenCache(children);
            toProcess = null;
        } else {
            log.info("Removing and setting");
            toProcess = workersCache.removedAndSet(children);
        }

        if (toProcess != null) {
            for (String worker : toProcess) {
                getAbsentWorkerTasks(worker);
            }
        }
    }

    private void getAbsentWorkerTasks(String worker) {
        zk.getChildren("/assign/" + worker, false, workerAssignmentCallback, null);
    }

    AsyncCallback.ChildrenCallback workerAssignmentCallback = new AsyncCallback.ChildrenCallback() {
        public void processResult(int rc, String path, Object ctx, List<String> children) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    getAbsentWorkerTasks(path);
                    break;
                case OK:
                    log.info("Successfully got a list of assignments: {} tasks", children.size());
                    for (String task : children) {
                        getDataReassign(path + "/" + task, task);
                    }
                    break;
                default:
                    log.error("getChildren failed", KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    private void getDataReassign(String path, String task) {
        zk.getData(path, getDataReassignCallback, task);
    }

    AsyncCallback.DataCallback getDataReassignCallback = new AsyncCallback.DataCallback() {
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    getDataReassign(path, (String) ctx);
                    break;
                case OK:
                    recreateTask(new RecreateTaskCtx(path, (String) ctx, data));
                    break;
                default:
                    log.error("Something went wrong when getting data ",
                            KeeperException.create(KeeperException.Code.get(rc)));
            }
        }
    };

    private void recreateTask(RecreateTaskCtx ctx) {
        zk.create("/tasks/" + ctx.getTask(),
                ctx.getData(),
                CreateMode.PERSISTENT,
                recreateTaskCallback,
                ctx);
    }

    AsyncCallback.StringCallback recreateTaskCallback = new AsyncCallback.StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    recreateTask((RecreateTaskCtx) ctx);
                    break;
                case OK:
                    deleteAssignment(((RecreateTaskCtx) ctx).getPath());
                    break;
                case NODEEXISTS:
                    log.info("Node exists already, but if it hasn't been deleted, " +
                            "then it will eventually, so we keep trying: " + path);
                    recreateTask((RecreateTaskCtx) ctx);
                    break;
                default:
                    log.error("Something wwnt wrong when recreating task",
                            KeeperException.create(KeeperException.Code.get(rc)));
            }
        }
    };

    private void deleteAssignment(String path) {
        zk.delete(path, taskDeletionCallback);
    }

    AsyncCallback.VoidCallback taskDeletionCallback = new AsyncCallback.VoidCallback() {
        public void processResult(int rc, String path, Object rtx) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    deleteAssignment(path);
                    break;
                case OK:
                    log.info("Task correctly deleted: " + path);
                    break;
                default:
                    log.error("Failed to delete task data" + KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    private void getTaskData(String task) {
        zk.getData("/tasks/" + task, taskDataCallback, task);
    }

    AsyncCallback.DataCallback taskDataCallback = new AsyncCallback.DataCallback() {
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    getTaskData((String) ctx);
                    break;
                case OK:
                    List<String> list = workersCache.getList();
                    if (!list.isEmpty()) {
                        String designatedWorker = list.get(random.nextInt(list.size()));
                        String assignmentPath = "/assign/" + designatedWorker + "/" + ctx;
                        log.info("Assignment path: {}", assignmentPath);
                        createAssignment(assignmentPath, data);
                    }
                    break;
                default:
                    log.error("Error when trying to get task data.",
                            KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    private void createAssignment(String path, byte[] data) {
        zk.create(path,
                data,
                CreateMode.PERSISTENT,
                assignTaskCallback,
                data);
    }

    AsyncCallback.StringCallback assignTaskCallback = new AsyncCallback.StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    createAssignment(path, (byte[]) ctx);
                    break;
                case OK:
                    log.info("Task assigned correctly: " + name);
                    String taskId = name.substring(name.lastIndexOf("/") + 1);
                    deleteTask(taskId);
                    break;
                case NODEEXISTS:
                    log.warn("Task already assigned");

                    break;
                default:
                    log.error("Error when trying to assign task.",
                            KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    private void deleteTask(String name) {
        zk.delete("/tasks/" + name, taskDeleteCallback);
    }

    AsyncCallback.VoidCallback taskDeleteCallback = new AsyncCallback.VoidCallback() {
        public void processResult(int rc, String path, Object ctx) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    deleteTask(path);

                    break;
                case OK:
                    log.info("Successfully deleted " + path);

                    break;
                case NONODE:
                    log.info("Task has been deleted already");

                    break;
                default:
                    log.error("Something went wrong here, " +
                            KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    AsyncCallback.StatCallback masterExistsCallback = new AsyncCallback.StatCallback() {
        public void processResult(int rc, String path, Object ctx, Stat stat) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    masterExists();
                    break;
                case OK:
                    break;
                case NONODE:
                    state = MasterStates.RUNNING;
                    runForMaster();
                    log.info("It sounds like the previous master is gone, so let's run for master again.");
                    break;
                default:
                    checkMaster();
                    break;
            }
        }
    };

    Watcher masterExistsWatcher = new Watcher() {
        public void process(WatchedEvent e) {
            if (e.getType() == Event.EventType.NodeDeleted) {
                runForMaster();
            }
        }
    };
}
