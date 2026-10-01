package com.example.Job_Scheduler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public boolean claim(Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow();

        if (job.getStatus() != JobStatus.PENDING) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(now);
        job.setUpdatedAt(now);

        jobRepository.saveAndFlush(job);

        return true;
    }
}