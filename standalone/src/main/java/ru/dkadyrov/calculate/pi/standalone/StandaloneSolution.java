package ru.dkadyrov.calculate.pi.standalone;

import lombok.extern.slf4j.Slf4j;
import ru.dkadyrov.calculate.pi.api.Calculator;
import ru.dkadyrov.calculate.pi.api.Solution;

import java.util.concurrent.CompletableFuture;

/**
 * This solution will request the approximate number of calculations to run in calculating π.
 * The calculations using Producer-Consumers pattern.
 * A Producer is a thread that generates jobs (messages) which are then executed by Consumer threads (workers).
 * Consumers thread calculate a sum of numbers.
 */
@Slf4j
public class StandaloneSolution implements Solution {

    private final Calculator calculator = new ProducerConsumerCalculator();

    @Override
    public CompletableFuture<Double> calculatePiAsync(int digits) {
        try {
            return CompletableFuture.completedFuture(calculator.calculate(digits));
        } catch (Exception ex) {
            CompletableFuture<Double> result = new CompletableFuture<>();
            result.completeExceptionally(ex);
            return result;
        }
    }
}
