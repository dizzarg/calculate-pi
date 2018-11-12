package ru.dkadyrov.calculate.pi.spring.master.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import ru.dkadyrov.calculate.pi.spring.master.model.Task;

@Repository
public interface TasksRepository extends ReactiveCrudRepository<Task, Long> {
}
