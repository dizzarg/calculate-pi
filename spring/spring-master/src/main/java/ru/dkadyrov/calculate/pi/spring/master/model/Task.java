package ru.dkadyrov.calculate.pi.spring.master.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document
public class Task {

    @Id
    private String id;

    private Integer digits;

    private Double result;

    private Date createdAt = new Date();

    private Date updatedAt;

}
