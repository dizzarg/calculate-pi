package ru.dkadyrov.calculate.pi.distributed.worker;

import java.util.concurrent.CountDownLatch;

public class WorkerApp {

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Worker worker = new Worker(args[0], args[1]);
        worker.run();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                worker.close();
                latch.countDown();
            }
        }));
        latch.await();
    }

}
