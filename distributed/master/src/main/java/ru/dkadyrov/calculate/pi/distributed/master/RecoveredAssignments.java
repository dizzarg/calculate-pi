package ru.dkadyrov.calculate.pi.distributed.master;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClient;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a task to recover assignments after
 * a primary master crash. The main idea is to determine the
 * tasks that have already been assigned and assign the ones
 * that haven't
 */
@Slf4j
public class RecoveredAssignments {
    /*
     * Various lists wew need to keep track of.
     */
    List<String> tasks;
    List<String> assignments;
    List<String> status;
    List<String> activeWorkers;
    List<String> assignedWorkers;

    RecoveryCallback cb;

    ZKClient zk;

    /**
     * Callback interface. Called once
     * recovery completes or fails.
     */
    public interface RecoveryCallback {
        final static int OK = 0;
        final static int FAILED = -1;

        public void recoveryComplete(int rc, List<String> tasks);
    }

    /**
     * Recover unassigned tasks.
     *
     * @param zk
     */
    RecoveredAssignments(ZKClient zk) {
        this.zk = zk;
        this.assignments = new ArrayList<>();
    }

    public static void recover(ZKClient zk, RecoveryCallback recoveryCallback) {
        RecoveredAssignments assignments = new RecoveredAssignments(zk);
        assignments.recover(recoveryCallback);
    }

    /**
     * Starts recovery.
     *
     * @param recoveryCallback
     */
    public void recover(RecoveryCallback recoveryCallback) {
        // Read task list with getChildren
        cb = recoveryCallback;
        getTasks();
    }

    private void getTasks() {
        zk.getChildren("/tasks", false, tasksCallback, null);
    }

    ChildrenCallback tasksCallback = new ChildrenCallback() {
        public void processResult(int rc, String path, Object ctx, List<String> children) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    getTasks();
                    break;
                case OK:
                    log.info("Getting tasks for recovery");
                    tasks = children;
                    getAssignedWorkers();
                    break;
                default:
                    log.error("getChildren failed", KeeperException.create(Code.get(rc), path));
                    cb.recoveryComplete(RecoveryCallback.FAILED, null);
            }
        }
    };

    private void getAssignedWorkers() {
        zk.getChildren("/assign", false, assignedWorkersCallback, null);
    }

    ChildrenCallback assignedWorkersCallback = new ChildrenCallback() {
        public void processResult(int rc, String path, Object ctx, List<String> children) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    getAssignedWorkers();
                    break;
                case OK:
                    assignedWorkers = children;
                    getWorkers(children);

                    break;
                default:
                    log.error("getChildren failed", KeeperException.create(Code.get(rc), path));
                    cb.recoveryComplete(RecoveryCallback.FAILED, null);
            }
        }
    };

    private void getWorkers(Object ctx) {
        zk.getChildren("/workers", false, workersCallback, ctx);
    }


    ChildrenCallback workersCallback = new ChildrenCallback() {
        public void processResult(int rc, String path, Object ctx, List<String> children) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    getWorkers(ctx);
                    break;
                case OK:
                    log.info("Getting worker assignments for recovery: " + children.size());
                    if (children.size() == 0) {
                        log.warn("Empty list of workers, possibly just starting");
                        cb.recoveryComplete(RecoveryCallback.OK, new ArrayList<String>());
                        break;
                    }
                    activeWorkers = children;
                    for (String s : assignedWorkers) {
                        getWorkerAssignments("/assign/" + s);
                    }
                    break;
                default:
                    log.error("getChildren failed", KeeperException.create(Code.get(rc), path));
                    cb.recoveryComplete(RecoveryCallback.FAILED, null);
            }
        }
    };

    private void getWorkerAssignments(String s) {
        zk.getChildren(s, false, workerAssignmentsCallback, null);
    }

    ChildrenCallback workerAssignmentsCallback = new ChildrenCallback() {
        public void processResult(int rc,
                                  String path,
                                  Object ctx,
                                  List<String> children) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    getWorkerAssignments(path);
                    break;
                case OK:
                    String worker = path.replace("/assign/", "");
                    if (activeWorkers.contains(worker)) {
                        assignments.addAll(children);
                    } else {
                        for (String task : children) {
                            if (!tasks.contains(task)) {
                                tasks.add(task);
                                getDataReassign(path, task);
                            } else {
                                deleteAssignment(path + "/" + task);
                            }
                            deleteAssignment(path);
                        }

                    }
                    assignedWorkers.remove(worker);
                    if (assignedWorkers.size() == 0) {
                        log.info("Getting statuses for recovery");
                        getStatuses();
                    }
                    break;
                case NONODE:
                    log.info("No such znode exists: " + path);
                    break;
                default:
                    log.error("getChildren failed", KeeperException.create(Code.get(rc), path));
                    cb.recoveryComplete(RecoveryCallback.FAILED, null);
            }
        }
    };

    /**
     * Get data of task being reassigned.
     *
     * @param path
     * @param task
     */
    void getDataReassign(String path, String task) {
        zk.getData(path, getDataReassignCallback, task);
    }

    /**
     * Get task data reassign callback.
     */
    DataCallback getDataReassignCallback = new DataCallback() {
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    getDataReassign(path, (String) ctx);

                    break;
                case OK:
                    recreateTask(new RecreateTaskCtx(path, (String) ctx, data));
                    break;
                default:
                    log.error("Something went wrong when getting data ",
                            KeeperException.create(Code.get(rc)));
            }
        }
    };

    /**
     * Recreate task znode in /tasks
     *
     * @param ctx Recreate text context
     */
    void recreateTask(RecreateTaskCtx ctx) {
        zk.create("/tasks/" + ctx.getTask(),
                ctx.getData(),
                CreateMode.PERSISTENT,
                recreateTaskCallback,
                ctx);
    }

    /**
     * Recreate znode callback
     */
    StringCallback recreateTaskCallback = new StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    recreateTask((RecreateTaskCtx) ctx);
                    break;
                case OK:
                    deleteAssignment(((RecreateTaskCtx) ctx).getPath());
                    break;
                case NODEEXISTS:
                    log.warn("Node shouldn't exist: " + path);

                    break;
                default:
                    log.error("Something wwnt wrong when recreating task",
                            KeeperException.create(Code.get(rc)));
            }
        }
    };

    /**
     * Delete assignment of absent worker
     *
     * @param path Path of znode to be deleted
     */
    void deleteAssignment(String path) {
        zk.delete(path, taskDeletionCallback);
    }

    VoidCallback taskDeletionCallback = new VoidCallback() {
        public void processResult(int rc, String path, Object rtx) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    deleteAssignment(path);
                    break;
                case OK:
                    log.info("Task correctly deleted: " + path);
                    break;
                default:
                    log.error("Failed to delete task data" +
                            KeeperException.create(Code.get(rc), path));
            }
        }
    };


    private void getStatuses() {
        zk.getChildren("/status", false, statusCallback, null);
    }

    private ChildrenCallback statusCallback = new ChildrenCallback() {
        public void processResult(int rc,
                                  String path,
                                  Object ctx,
                                  List<String> children) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    getStatuses();

                    break;
                case OK:
                    log.info("Processing assignments for recovery");
                    status = children;
                    processAssignments();

                    break;
                default:
                    log.error("getChildren failed", KeeperException.create(Code.get(rc), path));
                    cb.recoveryComplete(RecoveryCallback.FAILED, null);
            }
        }
    };

    private void processAssignments() {
        log.info("Size of tasks: " + tasks.size());
        // Process list of pending assignments
        for (String s : assignments) {
            log.info("Assignment: " + s);
            deleteAssignment("/tasks/" + s);
            tasks.remove(s);
        }

        log.info("Size of tasks after assignment filtering: " + tasks.size());

        for (String s : status) {
            log.info("Checking task: {} ", s);
            deleteAssignment("/tasks/" + s);
            tasks.remove(s);
        }
        log.info("Size of tasks after status filtering: " + tasks.size());

        // Invoke callback
        cb.recoveryComplete(RecoveryCallback.OK, tasks);
    }
}
