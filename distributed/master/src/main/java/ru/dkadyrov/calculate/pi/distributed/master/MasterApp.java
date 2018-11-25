package ru.dkadyrov.calculate.pi.distributed.master;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class MasterApp {

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Master master = new Master(args[0], args[1]);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                master.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            latch.countDown();
        }));
        master.run();
        latch.await();
    }

}
