package ru.dkadyrov.calculate.pi.distributed.master;

public class RecreateTaskCtx {

    private String path;
    private String task;
    private byte[] data;

    public RecreateTaskCtx(String path, String task, byte[] data) {
        this.path = path;
        this.task = task;
        this.data = data;
    }

    public String getPath() {
        return path;
    }

    public String getTask() {
        return task;
    }

    public byte[] getData() {
        return data;
    }
}
