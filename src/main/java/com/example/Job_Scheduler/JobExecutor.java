package com.example.Job_Scheduler;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class JobExecutor {

    private final JobRepository jobRepository;
    private final JobAction jobAction;

    public JobExecutor(
            JobRepository jobRepository,
            JobAction jobAction
    ) {
        this.jobRepository = jobRepository;
        this.jobAction = jobAction;
    }

    public void execute(Job job) {

        LocalDateTime startedAt = LocalDateTime.now();

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(startedAt);
        job.setUpdatedAt(startedAt);
        jobRepository.save(job);

        try {

            jobAction.execute(job);

            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {

            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(e.getMessage());
        }

        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }
}