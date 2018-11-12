package ru.dkadyrov.calculate.pi.standalone;

import lombok.extern.slf4j.Slf4j;
import ru.dkadyrov.calculate.pi.api.Calculator;
import ru.dkadyrov.calculate.pi.api.Solution;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * This solution will request the approximate number of calculations to run in calculating π.
 * The calculations using Producer-Consumers pattern.
 * A Producer is a thread that generates jobs (messages) which are then executed by Consumer threads (workers).
 * Consumers thread calculate a sum of numbers.
 */
@Slf4j
public class StandaloneSolution implements Solution, Closeable {

    private final ProducerConsumer<Integer, Double> producerConsumer;

    public StandaloneSolution(ExecutorService pool, Calculator calculator) {
        Integer size = Integer.getInteger("task.queue.size", 100);
        this.producerConsumer = new ProducerConsumer<>(size, pool, calculator::calculate);
    }

    @Override
    public CompletableFuture<Double> calculatePiAsync(int digits) {
        try {
            return producerConsumer.submit(digits);
        } catch (Exception ex) {
            CompletableFuture<Double> result = new CompletableFuture<>();
            result.completeExceptionally(ex);
            return result;
        }
    }

    @Override
    public void close() {
        try {
            producerConsumer.shutdown();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
