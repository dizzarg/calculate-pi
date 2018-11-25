package ru.dkadyrov.calculate.pi.distributed.worker;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;

import static ru.dkadyrov.calculate.pi.distributed.common.ClientFactory.newClient;
import static ru.dkadyrov.calculate.pi.distributed.common.Constants.WORKERS;

@Slf4j
public class Worker implements Closeable, Runnable {
    private String name;
    private String connectString;
    private CuratorFramework client;
    private PathChildrenCache tasks;
    private String path;

    public Worker(String name, String connectString) {
        this.name = name;
        this.connectString = connectString;
    }

    public void run() {

        try {
            client = newClient(connectString);
            client.start();
            path = client.create()
                    .withMode(CreateMode.PERSISTENT_SEQUENTIAL)
                    .forPath(WORKERS + "/worker-", name.getBytes(StandardCharsets.UTF_8));
            log.info("Created {} by path {}", name, path);

            tasks = new PathChildrenCache(client, path, true);
            tasks.getListenable().addListener((client, event) -> {
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                    String data = new String(event.getData().getData(), StandardCharsets.UTF_8);
                    String[] params = data.split(":");
                    int start = Integer.parseInt(params[0]);
                    int end = Integer.parseInt(params[1]);
                    double result = 0d;
                    for (int i = start; i < end; i++) {
                        result += next(i);
                    }
                    client.setData()
                            .forPath(event.getData().getPath(), Double.toString(result).getBytes(StandardCharsets.UTF_8));
                    log.info("Worker completed task: {} Path {}", result, event.getData().getPath());
                }
            });
            tasks.start();
        } catch (Exception e) {
            log.error("Error run {}", name, e);
        }
    }

    private static double next(int i) {
        return (i % 2 == 0 ? 4.0 : -4.0) / (2.0 * i + 1);
    }

    @Override
    public void close() {
        try {
            tasks.close();
            client.close();
        } catch (Exception e) {
            log.error("Failure to shutdown {}", name, e);
        }
    }
}