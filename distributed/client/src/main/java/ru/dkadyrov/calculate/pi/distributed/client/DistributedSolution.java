package ru.dkadyrov.calculate.pi.distributed.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.Stat;
import ru.dkadyrov.calculate.pi.api.Solution;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DistributedSolution implements Solution {
    private final ZKClient zk;
    private ConcurrentHashMap<String, Object> ctxMap = new ConcurrentHashMap<String, Object>();

    public DistributedSolution(ZKClient ZKClient) {
        zk = ZKClient;
    }

    @Override
    public CompletableFuture<Double> calculatePiAsync(int digits) {
        String task = Integer.toString(digits);
        TaskObject taskObject = new TaskObject(task);
        createTask(task, taskObject);
        return taskObject.getResult();
    }

    private void createTask(String task, TaskObject taskObject) {
        zk.create("/tasks/task-",
                task.getBytes(),
                CreateMode.PERSISTENT_SEQUENTIAL,
                createTaskCallback,
                taskObject);
    }

    void watchStatus(String path, Object ctx) {
        ctxMap.put(path, ctx);
        zk.exists(path,
                statusWatcher,
                existsCallback,
                ctx);
    }

    private StringCallback createTaskCallback = new StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    createTask(((TaskObject) ctx).getTask(), (TaskObject) ctx);
                    break;
                case OK:
                    watchStatus(name.replace("/tasks/", "/status/"), ctx);
                    break;
                default:
                    log.error("Something went wrong" + KeeperException.create(Code.get(rc), path));
            }
        }
    };

    private Watcher statusWatcher = new Watcher() {
        public void process(WatchedEvent e) {
            if (e.getType() == EventType.NodeCreated) {
                zk.getData(e.getPath(), getDataCallback, ctxMap.get(e.getPath()));
            }
        }
    };

    private StatCallback existsCallback = new StatCallback() {
        public void processResult(int rc, String path, Object ctx, Stat stat) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    watchStatus(path, ctx);
                    break;
                case OK:
                    if (stat != null) {
                        zk.getData(path, getDataCallback, ctx);
                        log.info("Status node is there: " + path);
                    }
                    break;
                case NONODE:
                    break;
                default:
                    log.error("Something went wrong when checking if the status node exists: ",
                            KeeperException.create(Code.get(rc), path));
                    break;
            }
        }
    };

    private DataCallback getDataCallback = new DataCallback() {
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    zk.getData(path, getDataCallback, ctxMap.get(path));
                    return;
                case OK:
                    String taskResult = new String(data);
                    log.info("Task {}, result = {} ", path, taskResult);

                    TaskObject taskObject = (TaskObject) ctx;
                    if (taskResult.isEmpty()) {
                        return;
                    }
                    taskObject.getResult().complete(Double.parseDouble(taskResult));
                    zk.delete(path, taskDeleteCallback);
                    ctxMap.remove(path);
                    break;
                case NONODE:
                    log.warn("Status node is gone!");
                    return;
                default:
                    log.error("Something went wrong here, {}", KeeperException.create(Code.get(rc), path));
            }
        }
    };

    private VoidCallback taskDeleteCallback = new VoidCallback() {
        public void processResult(int rc, String path, Object ctx) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    zk.delete(path, taskDeleteCallback);
                    break;
                case OK:
                    log.info("Successfully deleted " + path);
                    break;
                default:
                    log.error("Something went wrong here, {}", KeeperException.create(Code.get(rc), path));
            }
        }
    };
}
