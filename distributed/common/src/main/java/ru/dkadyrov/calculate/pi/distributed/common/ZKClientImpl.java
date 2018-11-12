package ru.dkadyrov.calculate.pi.distributed.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZKClientImpl implements Watcher, AutoCloseable, ZKClient {

    private ZooKeeper zk;
    private String hostPort;
    private boolean expired;
    CountDownLatch connectedLatch = new CountDownLatch(1);

    public ZKClientImpl(String hostPort) {
        this.hostPort = hostPort;
    }

    @Override
    public void connect() {
        try {
            zk = new ZooKeeper(hostPort, 15000, this);
            connectedLatch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(String path, byte[] data, CreateMode mode, AsyncCallback.StringCallback cb, Object ctx) {
        zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode, cb, ctx);
    }

    @Override
    public void exists(String path, Watcher watcher, AsyncCallback.StatCallback cb, Object ctx) {
        zk.exists(path, watcher, cb, ctx);
    }

    @Override
    public void getData(String path, AsyncCallback.DataCallback cb, Object ctx) {
        zk.getData(path, false, cb, ctx);
    }

    @Override
    public void delete(final String path, AsyncCallback.VoidCallback cb) {
        zk.delete(path, -1, cb, null);
    }

    @Override
    public void getChildren(String path, boolean watch, AsyncCallback.ChildrenCallback tasksCallback, Object ctx) {
        zk.getChildren(path, watch, tasksCallback, ctx);
    }

    @Override
    public void getChildren(String path, Watcher watcher, AsyncCallback.ChildrenCallback cb) {
        zk.getChildren(path, watcher, cb, null);
    }

    @Override
    public void setData(String path, byte[] data, AsyncCallback.StatCallback cb, Object ctx) {
        zk.setData(path, data, -1, cb, ctx);
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public void process(WatchedEvent e) {
        if (e.getType() == Event.EventType.None) {
            switch (e.getState()) {
                case SyncConnected:
                    connectedLatch.countDown();
                    break;
                case Disconnected:
                    connect();
                    break;
                case Expired:
                    expired = true;
                    log.debug("Exiting due to session expiration");
                default:
                    break;
            }
        }
    }

    @Override
    public void close() {
        log.info("Closing");
        try {
            zk.close();
        } catch (InterruptedException e) {
            log.warn("ZooKeeper interrupted while closing");
        }
    }

}
