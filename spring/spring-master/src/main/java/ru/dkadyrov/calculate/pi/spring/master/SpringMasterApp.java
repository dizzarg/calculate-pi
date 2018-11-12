package ru.dkadyrov.calculate.pi.spring.master;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.dkadyrov.calculate.pi.api.Solution;
import ru.dkadyrov.calculate.pi.distributed.client.DistributedSolution;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClient;
import ru.dkadyrov.calculate.pi.distributed.common.ZKClientImpl;

@SpringBootApplication
public class SpringMasterApp {

    @Bean(initMethod = "connect")
    public ZKClient zkClient(@Value("${zk.connection.string}") String connectionString) {
        return new ZKClientImpl(connectionString);
    }

    @Bean
    public Solution solution(ZKClient zkClient) {
        return new DistributedSolution(zkClient);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringMasterApp.class, args);
    }

}
