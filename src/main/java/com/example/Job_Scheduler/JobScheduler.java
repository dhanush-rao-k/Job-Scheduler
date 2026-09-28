package com.example.Job_Scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

    private final JobExecutor jobExecutor;
    private final JobRepository jobRepository;

    public JobScheduler(JobRepository jobRepository, JobExecutor jobExecutor) {
        this.jobRepository = jobRepository;
        this.jobExecutor = jobExecutor;
    }

    @Scheduled(fixedRate = 5000)
    public void findDueJobs() {

        LocalDateTime now = LocalDateTime.now();

        List<Job> dueJobs =
                jobRepository.findByStatusAndRunAtLessThanEqual(
                        JobStatus.PENDING,
                        now
                );

        for (Job job : dueJobs)
            jobExecutor.execute(job);
    }
}