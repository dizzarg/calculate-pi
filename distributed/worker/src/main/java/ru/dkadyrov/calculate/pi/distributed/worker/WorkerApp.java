package ru.dkadyrov.calculate.pi.distributed.worker;

import ru.dkadyrov.calculate.pi.api.Leibniz;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClient;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClientImpl;

public class WorkerApp {

    public static void main(String[] args) throws Exception {
        ZKClient zkClient = new ZKClientImpl(args[0]);
        Worker w = new Worker(zkClient, new Leibniz());
        zkClient.connect();

        w.bootstrap();
        w.register();

        w.getTasks();

        while (!zkClient.isExpired()) {
            Thread.sleep(1000);
        }

        zkClient.close();
    }

}
