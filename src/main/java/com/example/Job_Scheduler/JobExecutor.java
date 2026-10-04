package com.example.Job_Scheduler;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import org.springframework.stereotype.Component;

@Component
public class JobExecutor {

    private final JobRepository jobRepository;
    private final JobActionResolver jobActionResolver;
    private final BackoffCalculator backoffCalculator;
    private final IdempotencyService idempotencyService;
    private final JobExecutionService jobExecutionService;
    private final JobLeaseService jobLeaseService;

    public JobExecutor(JobRepository jobRepository, JobActionResolver jobActionResolver, BackoffCalculator backoffCalculator, IdempotencyService idempotencyService, JobExecutionService jobExecutionService , JobLeaseService jobLeaseService) {
        this.jobRepository = jobRepository;
        this.jobActionResolver = jobActionResolver;
        this.backoffCalculator = backoffCalculator;
        this.idempotencyService = idempotencyService;
        this.jobExecutionService = jobExecutionService;
        this.jobLeaseService = jobLeaseService;
    }

    public void execute(Long jobId, UUID executionToken) {
    Job job = jobRepository.findById(jobId).orElseThrow();

    if (!executionToken.equals(job.getExecutionToken())) {
        return;
    }

    String idempotencyKey = "job-" + job.getId();

    if (idempotencyService.isCompleted(idempotencyKey)) {
        return;
    }

    job.setAttempt(job.getAttempt() + 1);

    JobExecution execution =
            jobExecutionService.startExecution(
                    job.getId(),
                    job.getAttempt()
            );

    ScheduledFuture<?> leaseRenewal = jobLeaseService.startRenewal(job.getId(), executionToken);

    try {
        JobAction jobAction =
                jobActionResolver.resolve(job.getType());

        jobAction.execute(job);

        idempotencyService.complete(idempotencyKey);

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());

        jobExecutionService.completeExecution(execution);

    } catch (Exception e) {

        job.setErrorMessage(e.getMessage());

        jobExecutionService.failExecution(
                execution,
                e.getMessage()
        );

        if (job.getAttempt() < job.getMaxAttempts()) {

            long delayMilliseconds =
                    backoffCalculator.calculateBackoff(
                            job.getAttempt()
                    );

            job.setStatus(JobStatus.PENDING);

            job.setRunAt(
                    LocalDateTime.now()
                            .plusNanos(
                                    delayMilliseconds * 1_000_000L
                            )
            );

        } else {

            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
        }
    }finally{
        leaseRenewal.cancel(true);
        job.setExecutionToken(null);
    job.setLeaseUntil(null);
    job.setUpdatedAt(LocalDateTime.now());

    jobRepository.save(job);
    }
}
}