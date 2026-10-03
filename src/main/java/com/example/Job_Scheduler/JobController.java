package com.example.Job_Scheduler;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;  
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService jobService;
    private final JobExecutionService jobExecutionService;
    public JobController(JobService jobService, JobExecutionService jobExecutionService)
    {
        this.jobService=jobService;
        this.jobExecutionService=jobExecutionService;
    }
    @PostMapping
    public Job createJob(@RequestBody Job job)
    {
        return jobService.createJob(job);
    }
    @GetMapping
    public List<Job> getAllJobs()
    {
        return jobService.getAllJobs();
    }
    @GetMapping("/{id}")
    public Job getJob(@PathVariable Long id)
    {
        return jobService.getJob(id);
    }
    @GetMapping("/{jobId}/executions")
    public List<JobExecution> getExecutionHistory(@PathVariable Long jobId) {
        return jobExecutionService.getExecutions(jobId);
    }
    @PutMapping("/{id}")
    public Job updateJob(@PathVariable Long id,@RequestBody Job job)
    {
        return jobService.updateJob(id,job);
    }
    @DeleteMapping("/{id}")
    public Job deleteJob(@PathVariable Long id)
    {
        return jobService.deleteJob(id);
    }

}
