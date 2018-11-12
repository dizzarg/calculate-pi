package ru.dkadyrov.calculate.pi.distributed.worker;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import ru.dkadyrov.calculate.pi.api.Calculator;
import ru.dkadyrov.calculate.pi.distributed.common.ChildrenCache;
import ru.dkadyrov.calculate.pi.distributed.common.ServerIdGenerator;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClient;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
public class Worker {

    public Worker(ZKClient zkClient, Calculator calculator) {
        this.zk = zkClient;
        this.calculator = calculator;
    }

    private ZKClient zk;
    private Calculator calculator;
    private String serverId = ServerIdGenerator.nextId();
    private String name;
    private ChildrenCache assignedTasksCache = new ChildrenCache();
    private Executor executor = Executors.newSingleThreadExecutor();
    private String status;

    private AsyncCallback.DataCallback taskDataCallback = new AsyncCallback.DataCallback() {
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    zk.getData(path, taskDataCallback);
                    break;
                case OK:
                    executor.execute(() -> {
                        String digits = new String(data);
                        log.info("Executing your task: " + digits);

                        double calculatedValue = calculator.calculate(Integer.parseInt(digits));
                        String result = Double.toString(calculatedValue);

                        log.info("Task calculated {}: {}", digits, result);
                        storeTaskStatus(ctx, result);
                        deleteTaskData(ctx);
                    });
                    break;
                default:
                    log.error("Failed to get task data: ", KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    public void bootstrap() {
        createAssignNode();
    }

    private void createAssignNode() {
        zk.create("/assign/worker-" + serverId,
                new byte[0],
                CreateMode.PERSISTENT,
                new AsyncCallback.StringCallback() {
                    public void processResult(int rc, String path, Object ctx, String name) {
                        switch (KeeperException.Code.get(rc)) {
                            case CONNECTIONLOSS:
                                createAssignNode();
                                break;
                            case OK:
                                log.info("Assign node created");
                                break;
                            case NODEEXISTS:
                                log.warn("Assign node already registered");
                                break;
                            default:
                                log.error("Something went wrong: " + KeeperException.create(KeeperException.Code.get(rc), path));
                        }
                    }
                }
        );
    }


    public void register() {
        name = "worker-" + serverId;
        zk.create("/workers/" + name,
                "idle".getBytes(),
                CreateMode.EPHEMERAL,
                new AsyncCallback.StringCallback() {
                    public void processResult(int rc, String path, Object ctx, String name) {
                        switch (KeeperException.Code.get(rc)) {
                            case CONNECTIONLOSS:
                                register();
                                break;
                            case OK:
                                log.info("Registered successfully: " + serverId);

                                break;
                            case NODEEXISTS:
                                log.warn("Already registered: " + serverId);

                                break;
                            default:
                                log.error("Something went wrong: ",
                                        KeeperException.create(KeeperException.Code.get(rc), path));
                        }
                    }
                }
        );
    }

    public void getTasks() {
        zk.getChildren("/assign/worker-" + serverId,
                e -> {
                    if (e.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                        getTasks();
                    }
                },
                tasksGetChildrenCallback);
    }

    private AsyncCallback.ChildrenCallback tasksGetChildrenCallback = new AsyncCallback.ChildrenCallback() {
        public void processResult(int rc, String path, Object ctx, List<String> children) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    getTasks();
                    break;
                case OK:
                    if (children != null) {
                        executor.execute(new Runnable() {
                            List<String> children;
                            DataCallback cb;

                            public Runnable init(List<String> children, DataCallback cb) {
                                this.children = children;
                                this.cb = cb;

                                return this;
                            }

                            public void run() {
                                if (children == null) {
                                    return;
                                }
                                log.info("Looping into tasks");
                                setStatus("Working");
                                for (String task : children) {
                                    log.trace("New task: {}", task);
                                    zk.getData("/assign/worker-" + serverId + "/" + task, cb, task);
                                }
                            }
                        }.init(assignedTasksCache.addedAndSet(children), taskDataCallback));
                    }
                    break;
                default:
                    log.error("getChildren failed: {}", KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    synchronized private void updateStatus(String status) {
        if (status.equals(this.status)) {
            zk.setData(
                    "/workers/" + name,
                    status.getBytes(),
                    new AsyncCallback.StatCallback() {
                        public void processResult(int rc, String path, Object ctx, Stat stat) {
                            switch (KeeperException.Code.get(rc)) {
                                case CONNECTIONLOSS:
                                    updateStatus((String) ctx);
                                    return;
                            }
                        }
                    },
                    status
            );
        }
    }

    private void setStatus(String status) {
        this.status = status;
        updateStatus(status);
    }

    private void storeTaskStatus(Object ctx, String result) {
        zk.create(
                "/status/" + ctx,
                result.getBytes(),
                CreateMode.PERSISTENT,
                new AsyncCallback.StringCallback() {
                    public void processResult(int rc, String path, Object ctx, String name) {
                        switch (KeeperException.Code.get(rc)) {
                            case CONNECTIONLOSS:
                                storeTaskStatus(ctx, result);
                                break;
                            case OK:
                                log.info("Created status znode correctly: " + name);
                                break;
                            case NODEEXISTS:
                                log.warn("Node exists: " + path);
                                break;
                            default:
                                log.error("Failed to create task data: ", KeeperException.create(KeeperException.Code.get(rc), path));
                        }

                    }
                }
        );
    }

    private void deleteTaskData(Object ctx) {
        zk.delete("/assign/worker-" + serverId + "/" + ctx, new AsyncCallback.VoidCallback() {
            public void processResult(int rc, String path, Object rtx) {
                switch (KeeperException.Code.get(rc)) {
                    case CONNECTIONLOSS:
                        break;
                    case OK:
                        log.info("Task correctly deleted: " + path);
                        break;
                    default:
                        log.error("Failed to delete task data" + KeeperException.create(KeeperException.Code.get(rc), path));
                }
            }
        });
    }
}
