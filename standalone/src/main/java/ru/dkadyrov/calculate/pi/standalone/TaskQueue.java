package ru.dkadyrov.calculate.pi.standalone;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Simple task queue based on {@link java.util.concurrent.ArrayBlockingQueue}.
 *
 * @param <E>
 */
public class TaskQueue<E> {

    BlockingQueue<E> queue;
    volatile boolean alive = true;

    public TaskQueue(int length) {
        queue = new ArrayBlockingQueue<>(length);
    }

    public void enqueue(E value) throws InterruptedException {
        queue.put(value);
    }

    public E dequeue() throws InterruptedException {
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