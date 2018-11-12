package ru.dkadyrov.calculate.pi.distributed.master;

import ru.dkadyrov.calculate.pi.distributed.common.ZKClient;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClientImpl;

public class MasterApp {

    public static void main(String[] args) throws Exception {
        ZKClient zkClient = new ZKClientImpl(args[0]);
        zkClient.connect();
        Master master = new Master(zkClient);

        master.runForMaster();
        while (!zkClient.isExpired()) {
            Thread.sleep(1000);
        }
        zkClient.close();
    }

}
