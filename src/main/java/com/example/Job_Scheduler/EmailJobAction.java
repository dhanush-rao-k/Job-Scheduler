package com.example.Job_Scheduler;

import org.springframework.stereotype.Component;

@Component
public class EmailJobAction implements JobAction {

    @Override
    public void execute(Job job) {
        System.out.println("Executing email job: " + job.getId());
    }
}