package ru.dkadyrov.calculate.pi.spring.master;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import ru.dkadyrov.calculate.pi.spring.master.model.Task;
import ru.dkadyrov.calculate.pi.spring.master.rest.TasksRest;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringMasterAppTest {

    @Autowired
    private TasksRest tasksRest;

    @Test
    public void shouldReturnTaskList_ok() {
        Flux<Task> list = tasksRest.list();
        Assert.assertNotNull(list);
    }

}