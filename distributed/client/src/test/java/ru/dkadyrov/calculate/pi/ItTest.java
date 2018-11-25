package ru.dkadyrov.calculate.pi;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Test;
import ru.dkadyrov.calculate.pi.distributed.client.DistributedSolution;
import ru.dkadyrov.calculate.pi.distributed.master.Master;
import ru.dkadyrov.calculate.pi.distributed.worker.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ItTest {

    @Test
    public void calculate_success() throws Exception {
        TestingServer server = new TestingServer();
        String connectString = server.getConnectString();
        Master master = new Master("Master", connectString);
        master.run();
        List<Worker> workers = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            Worker worker = new Worker("Worker#" + i, connectString);
            pool.submit(worker);
            workers.add(worker);
        }

        try (DistributedSolution solution = new DistributedSolution(connectString)) {
            solution.start();
            long start = System.nanoTime();
            solution.calculatePiAsync(6)
                    .thenAccept(result -> {
                        long duration = System.nanoTime() - start;
                        log.info("Calculated PI  {}", String.format("%.6f", result));
                        log.info("Math.PI        {}", String.format("%.6f", Math.PI));
                        log.info("Calculate time {}ms", TimeUnit.NANOSECONDS.toMillis(duration));
                        Assert.assertEquals(Math.PI, result, 0.0001);
                    }).get();
        }
    }
}
