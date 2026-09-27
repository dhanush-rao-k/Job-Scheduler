package com.example.Job_Scheduler;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController; 

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService jobService;
    public JobController(JobService jobService)
    {
        this.jobService=jobService;
    }
    @PostMapping
    public Job createJob(@RequestBody Job job)
    {
        return jobService.createJob(job);
    }
}
