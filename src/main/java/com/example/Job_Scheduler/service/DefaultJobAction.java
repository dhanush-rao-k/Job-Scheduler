package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;

import org.springframework.stereotype.Component;

@Component
public class DefaultJobAction implements JobAction {

    @Override
    public void execute(Job job) {
        System.out.println(
                "Executing job: " + job.getId() +
                " - " + job.getName()
        );
    }
}

