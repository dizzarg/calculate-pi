package ru.dkadyrov.calculate.pi.distributed.master;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.zookeeper.CreateMode;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ru.dkadyrov.calculate.pi.distributed.common.ClientFactory.newClient;
import static ru.dkadyrov.calculate.pi.distributed.common.Constants.*;

@Slf4j
public class Master implements Closeable, LeaderSelectorListener {

    private String name;
    private String connectString;
    private CuratorFramework client;
    private LeaderSelector master;
    private PathChildrenCache workers;
    private PathChildrenCache tasks;

    public Master(String name, String connectString) {
        this.name = name;
        this.connectString = connectString;
    }

    public void run() {
        try {
            client = newClient(connectString);
            client.start();
            client.create().forPath(MASTER);
            client.create().forPath(WORKERS);
            client.create().forPath(TASKS);
            master = new LeaderSelector(client, MASTER, this);
            master.start();
            tasks = new PathChildrenCache(client, TASKS, true);
            workers = new PathChildrenCache(client, WORKERS, true);
        } catch (Exception ex) {
            log.error("{} is broken", name, ex);
        }
    }

    @Override
    public void close() throws IOException {
        tasks.close();
        workers.close();
        master.close();
        client.create();
    }

    @Override
    public void takeLeadership(CuratorFramework curatorFramework) throws Exception {
        log.info("I am a master: " + name);
        curatorFramework.setData().forPath(MASTER, name.getBytes(StandardCharsets.UTF_8));
        Supplier<List<String>> workersSupplier = () ->
                workers.getCurrentData()
                        .stream()
                        .map(ChildData::getPath)
                        .collect(Collectors.toList());
        workers.start();
        tasks.getListenable().addListener((client, event) -> {
            if (event.getType() != PathChildrenCacheEvent.Type.CHILD_ADDED) {
                return;
            }
            int digits = Integer.parseInt(new String(event.getData().getData()));
            int j = 0;
            int worker = 0;
            int step = 100_000;
            double member = next(j);
            List<CompletableFuture<Double>> futures = new ArrayList<>();
            while (Math.abs(member) > Math.pow(10, -digits)) {
                String currentWorker = workersSupplier.get().get(worker % workersSupplier.get().size());
                String taskPath = currentWorker + "/task-";
                CompletableFuture<String> future = new CompletableFuture<>();
                String format = String.format("%d:%d", j, j + step);
                String node = client.create()
                        .withMode(CreateMode.PERSISTENT_SEQUENTIAL)
                        .forPath(taskPath, format.getBytes(StandardCharsets.UTF_8));
                NodeCache nodeCache = new NodeCache(client, node);
                nodeCache.getListenable().addListener(() -> {
                    log.info("Master see completed task: {}", node);
                    String data = new String(client.getData().forPath(node));
                    if (!data.contains(":")) {
                        future.complete(node);
                    }
                });
                nodeCache.start();
                futures.add(future.thenApplyAsync(path -> {
                    try {
                        double result = Double.parseDouble(new String(client.getData().forPath(path)));
                        log.info("Worker executed. Result={}", result);
                        return result;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }));
                worker++;
                j += step;
                member = next(j);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenAcceptAsync(any -> {
                        try {
                            double sum = futures.stream().mapToDouble(CompletableFuture::join).sum();
                            String path = event.getData().getPath();
                            client.setData()
                                    .forPath(path, Double.toString(sum).getBytes(StandardCharsets.UTF_8));
                            log.info("Sum: {} Path: {}", sum, path);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    })
                    .exceptionally(ex -> {
                        log.error(ex.getMessage(), ex);
                        return null;
                    });
            log.info("Completed all tasks");
        });
        tasks.start();
    }

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        log.info("connectionState: " + connectionState);
    }

    private static double next(int i) {
        return (i % 2 == 0 ? 4.0 : -4.0) / (2.0 * i + 1);
    }

}
