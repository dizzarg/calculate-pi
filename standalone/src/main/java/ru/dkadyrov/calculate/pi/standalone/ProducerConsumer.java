package ru.dkadyrov.calculate.pi.standalone;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Producer-Consumers pattern implemenation
 *
 * @param <MESSAGE> incoming message type
 * @param <RESULT>  executed result type
 */
@Slf4j
public class ProducerConsumer<MESSAGE, RESULT> {

    public static final String CONSUMER_POOL_AWAIT_TERMINATION_MS = "consumer.pool.await.termination.ms";
    private final ExecutorService pool;

    enum EventType {
        INVOCATION, COMPLETION
    }

    class Event {
        MESSAGE value;
        EventType eventType;
        CompletableFuture<RESULT> result;
    }

    private TaskQueue<Event> queue;
    private boolean alive = true;

    public ProducerConsumer(int size, ExecutorService pool, Consumer<MESSAGE, RESULT> consumer) {
        this.queue = new TaskQueue<>(size);
        this.pool = pool;
        pool.submit(() -> {
            while (alive || queue.count() > 0) {
                Event e = queue.dequue();
                switch (e.eventType) {
                    case INVOCATION:
                        e.result.complete(consumer.compute(e.value));
                        break;
                    case COMPLETION:
                        alive = false;
                        consumer.onComplete();
                }
            }
            queue.shutdown();
            return true;
        });
    }

    public CompletableFuture<RESULT> submit(MESSAGE message) throws InterruptedException {
        Event event = new Event();
        event.value = message;
        event.eventType = EventType.INVOCATION;
        event.result = new CompletableFuture<>();
        queue.enquue(event);
        return event.result;
    }

    public void shutdown() throws InterruptedException {
        Event event = new Event();
        event.eventType = EventType.COMPLETION;
        queue.enquue(event);
        pool.shutdown();
        Integer timeout = Integer.getInteger(CONSUMER_POOL_AWAIT_TERMINATION_MS, 1000);
        if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
            pool.shutdownNow();
        }
    }

}
