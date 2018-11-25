package ru.dkadyrov.calculate.pi.distributed.client;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DistributedSolutionApp {

    public static void main(String[] args) throws Exception {
        try (DistributedSolution solution = new DistributedSolution(args[0])) {
            solution.start();
            long start = System.nanoTime();
            solution.calculatePiAsync(Integer.parseInt(args[1]))
                    .thenAccept(result -> {
                        long duration = System.nanoTime() - start;
                        log.info("Calculated PI  {}", String.format("%.6f", result));
                        log.info("Math.PI        {}", String.format("%.6f", Math.PI));
                        log.info("Calculate time {}ms", TimeUnit.NANOSECONDS.toMillis(duration));
                    }).get();
        }

    }

}
