package ru.dkadyrov.calculate.pi.distributed.client;

import lombok.extern.slf4j.Slf4j;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClient;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClientImpl;

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Slf4j
public class DistributedSolutionApp {

    public static void main(String[] args) throws Exception {
        ZKClient zkClient = new ZKClientImpl(args[0]);
        DistributedSolution solution = new DistributedSolution(zkClient);
        zkClient.connect();

        CompletableFuture[] tasks = IntStream.range(0, 4)
                .mapToObj(i ->
                        solution.calculatePiAsync(i + 5).thenAcceptAsync(res -> log.info("Task completed. Result = {}", res))
                ).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(tasks).join();

        zkClient.close();

    }

}
