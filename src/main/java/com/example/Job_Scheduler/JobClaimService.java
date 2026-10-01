package com.example.Job_Scheduler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;
    private static final long LEASE_MINUTES = 5;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public UUID claim(Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow();

        if (job.getStatus() != JobStatus.PENDING) {
            return  null;
        }

        LocalDateTime now = LocalDateTime.now();

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(now);
        job.setUpdatedAt(now);
        job.setLeaseUntil(now.plusMinutes(LEASE_MINUTES));
        job.setExecutionToken(UUID.randomUUID());

        jobRepository.saveAndFlush(job);

        return job.getExecutionToken();
    }
}