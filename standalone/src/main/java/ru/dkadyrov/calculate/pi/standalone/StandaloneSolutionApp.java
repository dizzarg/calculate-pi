package ru.dkadyrov.calculate.pi.standalone;

import lombok.extern.slf4j.Slf4j;
import ru.dkadyrov.calculate.pi.api.Leibniz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class StandaloneSolutionApp {

    public static void main(String[] args) {
        Integer size = Integer.getInteger("consumer.pool.size", 5);
        ExecutorService pool = Executors.newFixedThreadPool(size);
        StandaloneSolution solution = new StandaloneSolution(pool, new Leibniz());
        solution.calculatePiAsync(Integer.parseInt(args[0]))
                .thenAccept(result -> {
                    log.info("Calculated PI {}", String.format("%.6f", result));
                    log.info("Math.PI       {}", String.format("%.6f", Math.PI));
                    solution.close();
                });
    }

}
