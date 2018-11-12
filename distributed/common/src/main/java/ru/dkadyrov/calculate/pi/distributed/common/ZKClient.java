package ru.dkadyrov.calculate.pi.distributed.common;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;

public interface ZKClient extends AutoCloseable {

    void connect();

    void create(String path, byte[] data, CreateMode mode, AsyncCallback.StringCallback cb, Object ctx);

    default void create(String path, byte[] data, CreateMode mode, AsyncCallback.StringCallback cb) {
        create(path, data, mode, cb, null);
    }

    void exists(String path, Watcher watcher, AsyncCallback.StatCallback cb, Object ctx);

    default void exists(String path, Watcher watcher, AsyncCallback.StatCallback cb) {
        exists(path, watcher, cb, null);
    }

    void getData(String path, AsyncCallback.DataCallback cb, Object ctx);

    default void getData(String path, AsyncCallback.DataCallback cb) {
        getData(path, cb);
    }

    void delete(String path, AsyncCallback.VoidCallback cb);

    void getChildren(String path, boolean watch, AsyncCallback.ChildrenCallback cb, Object ctx);

    void getChildren(String path, Watcher watcher, AsyncCallback.ChildrenCallback cb);

    void setData(String path, byte[] data, AsyncCallback.StatCallback cb, Object ctx);

    boolean isExpired();
}
