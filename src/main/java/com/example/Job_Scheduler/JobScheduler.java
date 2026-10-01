package com.example.Job_Scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class JobScheduler {

    private final JobRepository jobRepository;
    private final JobService jobService;
    private final JobExecutor jobExecutor;
    private final JobRecoveryService jobRecoveryService;
    private final ExecutorService executor =Executors.newFixedThreadPool(2);

    public JobScheduler(JobRepository jobRepository,JobService jobService,JobExecutor jobExecutor,JobRecoveryService jobRecoveryService)
    {
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.jobExecutor = jobExecutor;
        this.jobRecoveryService = jobRecoveryService;
    }

    @Scheduled(fixedRate = 5000)
    public void findDueJobs() {

        LocalDateTime now = LocalDateTime.now();


        List<Job> dueJobs =jobRepository.findByStatusAndRunAtLessThanEqual(JobStatus.PENDING,now);

        for (Job job : dueJobs) {
            UUID executionToken = jobService.claimJob(job.getId());

            if (executionToken != null) {
                executor.submit(() -> jobExecutor.execute(job.getId(), executionToken));
            }
        }
    }

    @PreDestroy
    public void shutdown() {
    executor.shutdown();
}
}