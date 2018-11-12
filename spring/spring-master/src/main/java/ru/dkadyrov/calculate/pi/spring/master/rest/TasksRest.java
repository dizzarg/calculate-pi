package ru.dkadyrov.calculate.pi.spring.master.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.dkadyrov.calculate.pi.api.Solution;
import ru.dkadyrov.calculate.pi.spring.master.model.Task;
import ru.dkadyrov.calculate.pi.spring.master.repository.TasksRepository;

import java.util.Date;

@RestController
@RequestMapping("/tasks")
public class TasksRest {

    @Autowired
    private TasksRepository reportRepository;

    @Autowired
    private Solution solution;

    @GetMapping
    public Flux<Task> list() {
        return reportRepository.findAll();
    }

    @PostMapping
    public Mono<Task> create(@RequestBody Task taskReport) {
        return reportRepository.save(taskReport)
                .flatMap(this::submitTask)
                .flatMap(completedTask -> reportRepository.save(completedTask));
    }

    private Mono<Task> submitTask(Task task) {
        return Mono.fromCompletionStage(solution.calculatePiAsync(task.getDigits())).map(result -> {
            task.setResult(result);
            task.setUpdatedAt(new Date());
            return task;
        });
    }
}
