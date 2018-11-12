package ru.dkadyrov.calculate.pi.distributed.common;

import java.util.Random;

public class ServerIdGenerator {

    public static String nextId() {
        return Integer.toHexString((new Random()).nextInt());
    }

}
