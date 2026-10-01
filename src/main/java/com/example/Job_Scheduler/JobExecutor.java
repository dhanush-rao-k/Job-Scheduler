package com.example.Job_Scheduler;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class JobExecutor {

    private final JobRepository jobRepository;
    private final JobActionResolver jobActionResolver;

    public JobExecutor(JobRepository jobRepository, JobActionResolver jobActionResolver) {
        this.jobRepository = jobRepository;
        this.jobActionResolver = jobActionResolver;
    }

    public void execute(Long jobId) {
    Job job = jobRepository.findById(jobId)
            .orElseThrow();

    try {
        JobAction jobAction = jobActionResolver.resolve(job.getType());
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