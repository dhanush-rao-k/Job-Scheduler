package com.example.Job_Scheduler;

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

