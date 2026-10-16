
package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.repo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;
    private static final long LEASE_MINUTES = 5;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Optional<ClaimedJob> claimNextJob() {
        LocalDateTime now = LocalDateTime.now();

        Optional<Job> candidate =
                jobRepository.findNextDueJobForUpdateSkipLocked(now);

        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        Job job = candidate.get();
        UUID executionToken = UUID.randomUUID();

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(now);
        job.setUpdatedAt(now);
        job.setLeaseUntil(now.plusMinutes(LEASE_MINUTES));
        job.setExecutionToken(executionToken);

        jobRepository.saveAndFlush(job);

        return Optional.of(
                new ClaimedJob(job.getId(), executionToken)
        );
    }

    public record ClaimedJob(Long jobId, UUID executionToken) {}
}
