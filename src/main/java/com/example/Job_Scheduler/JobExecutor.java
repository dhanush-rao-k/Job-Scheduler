package com.example.Job_Scheduler;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class JobExecutor {

    private final JobRepository jobRepository;

    public JobExecutor(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void execute(Job job) {
        job.setStatus(JobStatus.RUNNING);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
        try
        {
            // Simulate job execution
            System.out.println("Executing job: " + job.getId() + " - " + job.getName());
            job.setStatus(JobStatus.COMPLETED);
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
        }
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }
}