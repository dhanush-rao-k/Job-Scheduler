
package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.service.JobClaimService.ClaimedJob;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class JobScheduler {

    private static final int MAX_CONCURRENT_JOBS = 2;

    private final JobService jobService;
    private final JobExecutor jobExecutor;
    private final JobRecoveryService jobRecoveryService;

    private final Semaphore executionSlots =
            new Semaphore(MAX_CONCURRENT_JOBS);

    private final ExecutorService executor =
            new ThreadPoolExecutor(
                    MAX_CONCURRENT_JOBS,
                    MAX_CONCURRENT_JOBS,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(10),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    public JobScheduler(
            JobService jobService,
            JobExecutor jobExecutor,
            JobRecoveryService jobRecoveryService
    ) {
        this.jobService = jobService;
        this.jobExecutor = jobExecutor;
        this.jobRecoveryService = jobRecoveryService;
    }

    @Scheduled(fixedRate = 5000)
    public void findDueJobs() {
        jobRecoveryService.recoverExpiredJobs();

        while (executionSlots.tryAcquire()) {
            boolean handedOff = false;

            try {
                ClaimedJob claimedJob = jobService.claimNextJob();

                if (claimedJob == null) {
                    return;
                }

                executor.submit(() -> {
                    try {
                        jobExecutor.execute(
                                claimedJob.jobId(),
                                claimedJob.executionToken()
                        );
                    } finally {
                        executionSlots.release();
                    }
                });

                handedOff = true;
            } finally {
                if (!handedOff) {
                    executionSlots.release();
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
