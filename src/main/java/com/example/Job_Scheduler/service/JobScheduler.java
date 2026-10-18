package com.example.Job_Scheduler.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

    private final JobDispatchService jobDispatchService;
    private final JobRecoveryService jobRecoveryService;

    public JobScheduler(
            JobDispatchService jobDispatchService,
            JobRecoveryService jobRecoveryService
    ) {
        this.jobDispatchService = jobDispatchService;
        this.jobRecoveryService = jobRecoveryService;
    }

    @Scheduled(fixedRateString = "${job.scheduler.dispatch-rate-ms:5000}")
    public void dispatchJobs() {
        jobRecoveryService.recoverExpiredJobs();
        jobDispatchService.dispatchDueJobs();
    }
}
