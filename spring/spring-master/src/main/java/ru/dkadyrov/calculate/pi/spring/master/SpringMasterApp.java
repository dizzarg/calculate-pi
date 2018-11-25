package ru.dkadyrov.calculate.pi.spring.master;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.dkadyrov.calculate.pi.api.Solution;
import ru.dkadyrov.calculate.pi.distributed.client.DistributedSolution;

@SpringBootApplication
public class SpringMasterApp {

    @Bean
    public Solution solution(@Value("${zk.connection.string}") String connectionString) {
        return new DistributedSolution(connectionString);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringMasterApp.class, args);
    }

}
