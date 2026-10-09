package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.repo.*;

import java.time.LocalDateTime;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service 
public class JobRecoveryService {

    private final JobRepository jobRepository;

    public JobRecoveryService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public void recoverExpiredJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> expiredJobs = jobRepository.findByStatusAndLeaseUntilBefore(JobStatus.RUNNING, now);
        for (Job job : expiredJobs) {
            job.setStatus(JobStatus.PENDING);
            job.setExecutionToken(null);
            job.setLeaseUntil(null);
            job.setUpdatedAt(now);
            jobRepository.saveAndFlush(job);
        }
    } 


    
}
