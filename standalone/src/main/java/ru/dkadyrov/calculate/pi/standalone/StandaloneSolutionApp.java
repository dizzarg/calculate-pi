package ru.dkadyrov.calculate.pi.standalone;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * The Simple standalone application will calculate the approximate value of calculations π.
 */
@Slf4j
public class StandaloneSolutionApp {

    public static void main(String[] args) {
        long start = System.nanoTime();
        StandaloneSolution solution = new StandaloneSolution();
        solution.calculatePiAsync(Integer.parseInt(args[0]))
                .thenAccept(result -> {
                    long duration = System.nanoTime() - start;
                    log.info("Calculated PI  {}", String.format("%.6f", result));
                    log.info("Math.PI        {}", String.format("%.6f", Math.PI));
                    log.info("Calculate time {}ms", TimeUnit.NANOSECONDS.toMillis(duration));
                });
    }

}
