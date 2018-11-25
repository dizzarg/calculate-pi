package ru.dkadyrov.calculate.pi.distributed.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import ru.dkadyrov.calculate.pi.api.Solution;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static ru.dkadyrov.calculate.pi.distributed.common.ClientFactory.newClient;
import static ru.dkadyrov.calculate.pi.distributed.common.Constants.TASKS;

@Slf4j
public class DistributedSolution implements Solution, Closeable {
    private CuratorFramework client;
    private String connectString;

    public DistributedSolution(String connectString) {
        this.connectString = connectString;
    }

    public void start() {
        client = newClient(connectString);
        client.start();
    }

    @Override
    public CompletableFuture<Double> calculatePiAsync(int digits) {
        try {
            return executeTask(digits);
        } catch (Exception e) {
            CompletableFuture<Double> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private CompletableFuture<Double> executeTask(int inputValue) throws Exception {
        CompletableFuture<Double> future = new CompletableFuture<>();
        String path = client.create()
                .withMode(CreateMode.PERSISTENT_SEQUENTIAL)
                .forPath(TASKS + "/task-", Integer.toString(inputValue).getBytes(StandardCharsets.UTF_8));

        log.info("Create new task node: {}", path);

        client.checkExists().usingWatcher((CuratorWatcher) watchedEvent -> {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                byte[] data = client.getData().forPath(watchedEvent.getPath());
                String result = new String(data, StandardCharsets.UTF_8);
                log.info("Task executed: inputValue={}, result={}", inputValue, result);
                future.complete(Double.parseDouble(result));
            }
        }).forPath(path);

        return future;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
