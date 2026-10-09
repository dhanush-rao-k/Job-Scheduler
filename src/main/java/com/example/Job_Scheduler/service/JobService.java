package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.repo.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class JobService {
    private final JobRepository jobRepository;

    private final JobClaimService jobClaimService;

    public JobService(JobRepository jobRepository, JobClaimService jobClaimService)
    {
        this.jobRepository=jobRepository;
        this.jobClaimService=jobClaimService;
    }

    public Job createJob(Job job)
    {
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return jobRepository.save(job);
    }

    public List<Job> getAllJobs()
    {
        return jobRepository.findAll();
    }

    public Job getJob(Long id)
    {
        return jobRepository.findById(id).get();
    }
    
    public Job updateJob(Long id, Job job)
    {
        Job oldJob = jobRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Job not found"));

        oldJob.setName(job.getName());
        oldJob.setType(job.getType());
        oldJob.setStatus(job.getStatus());
        oldJob.setRunAt(job.getRunAt());
        oldJob.setPayload(job.getPayload());
        oldJob.setUpdatedAt(LocalDateTime.now());

        return jobRepository.save(oldJob);
    }

    public Job deleteJob(Long id)
    {
        Job job = jobRepository.findById(id).get();
        jobRepository.deleteById(id);
        return job;
    }

   public UUID claimJob(Long jobId) {
        try {
            return jobClaimService.claim(jobId);
        } catch (ObjectOptimisticLockingFailureException e) {
            return null;
        }
    }
}