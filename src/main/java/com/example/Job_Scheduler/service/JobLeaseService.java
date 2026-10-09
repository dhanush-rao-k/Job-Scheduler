package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.repo.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobLeaseService {

    private static final long LEASE_MINUTES = 5;
    private static final long RENEWAL_INTERVAL_MINUTES = 1;

    private final JobRepository jobRepository;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    public JobLeaseService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public boolean renewLease(Long jobId, UUID executionToken) {

        Job job = jobRepository.findById(jobId).orElse(null);

        if (job == null
                || job.getStatus() != JobStatus.RUNNING
                || !executionToken.equals(job.getExecutionToken())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (job.getLeaseUntil() == null
                || !job.getLeaseUntil().isAfter(now)) {
            return false;
        }

        job.setLeaseUntil(now.plusMinutes(LEASE_MINUTES));
        job.setUpdatedAt(now);

        jobRepository.saveAndFlush(job);

        return true;
    }

    public ScheduledFuture<?> startRenewal(
            Long jobId,
            UUID executionToken) {

        return scheduler.scheduleAtFixedRate(
                () -> renewLease(jobId, executionToken),
                RENEWAL_INTERVAL_MINUTES,
                RENEWAL_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}