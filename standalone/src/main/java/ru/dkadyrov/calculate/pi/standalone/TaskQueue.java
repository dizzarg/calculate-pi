package ru.dkadyrov.calculate.pi.standalone;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TaskQueue<E> {

    BlockingQueue<E> queue;
    volatile boolean alive = true;

    public TaskQueue(int length) {
        queue = new ArrayBlockingQueue<>(length);
    }

    public void enquue(E value) throws InterruptedException {
        queue.put(value);
    }

    public E dequue() throws InterruptedException {
        if (!alive) {
            Thread.currentThread().interrupt();
        }
        return queue.take();
    }

    public int count() {
        return queue.size();
    }

    public void shutdown() {
        alive = false;
    }

}