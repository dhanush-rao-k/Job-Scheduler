package com.example.Job_Scheduler;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
public class JobService {
    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository)
    {
        this.jobRepository=jobRepository;
    }

    public Job createJob(Job job)
    {
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }
}