package ru.dkadyrov.calculate.pi.distributed.common;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import static ru.dkadyrov.calculate.pi.distributed.common.Constants.NAMESPACE;

public class ClientFactory {

    public static CuratorFramework newClient(String connectString) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 5);
        return CuratorFrameworkFactory.builder()
                .namespace(NAMESPACE)
                .connectString(connectString)
                .retryPolicy(retryPolicy)
                .build();
    }

}
