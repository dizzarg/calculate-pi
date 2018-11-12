package ru.dkadyrov.calculate.pi.distributed.client;

import lombok.Value;

import java.util.concurrent.CompletableFuture;

@Value
public class TaskObject {

    private String task;
    private CompletableFuture<Double> result = new CompletableFuture<>();

}
