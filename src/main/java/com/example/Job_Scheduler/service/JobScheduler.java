package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class JobScheduler {

    private final JobService jobService;
    private final JobExecutor jobExecutor;
    private final JobRecoveryService jobRecoveryService;
    private final ExecutorService executor =new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(10),new ThreadPoolExecutor.CallerRunsPolicy());
    public JobScheduler(JobService jobService,JobExecutor jobExecutor,JobRecoveryService jobRecoveryService)
    {
        this.jobService = jobService;
        this.jobExecutor = jobExecutor;
        this.jobRecoveryService = jobRecoveryService;
    }

    @Scheduled(fixedRate = 5000)
    public void findDueJobs() {
        jobRecoveryService.recoverExpiredJobs();

        while (true) {
            JobClaimService.ClaimedJob claimedJob = jobService.claimNextJob();

            if (claimedJob == null) {
                return;
            }

            executor.submit(() -> jobExecutor.execute(
                    claimedJob.jobId(),
                    claimedJob.executionToken()
            ));
        }
    }

    @PreDestroy
    public void shutdown() {
    executor.shutdown();
}
}