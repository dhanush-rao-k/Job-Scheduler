package com.example.Job_Scheduler;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobExecutor {

    private final JobRepository jobRepository;
    private final JobActionResolver jobActionResolver;
    private final BackoffCalculator backoffCalculator;

    public JobExecutor(JobRepository jobRepository, JobActionResolver jobActionResolver, BackoffCalculator backoffCalculator) {
        this.jobRepository = jobRepository;
        this.jobActionResolver = jobActionResolver;
        this.backoffCalculator = backoffCalculator;
    }

    @Transactional
    public void execute(Long jobId, UUID executionToken) {
    Job job = jobRepository.findById(jobId)
            .orElseThrow();

    if (!executionToken.equals(job.getExecutionToken())) {
        return;
    }

    job.setAttempt(job.getAttempt() + 1);
    job.setUpdatedAt(LocalDateTime.now());
    jobRepository.save(job);

    try {
        JobAction jobAction = jobActionResolver.resolve(job.getType());
        jobAction.execute(job);

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());

    } catch (Exception e) {
        job.setErrorMessage(e.getMessage());

        if (job.getAttempt() < job.getMaxAttempts()) {
            long delayMilliseconds =
                    backoffCalculator.calculateBackoff(job.getAttempt());

            job.setStatus(JobStatus.PENDING);
            job.setRunAt(
                    LocalDateTime.now().plusSeconds(delayMilliseconds / 1000)
            );
        } else {
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
        }
    }

    // Release ownership of this execution
    job.setExecutionToken(null);
    job.setLeaseUntil(null);
    job.setUpdatedAt(LocalDateTime.now());

    jobRepository.save(job);
}
}