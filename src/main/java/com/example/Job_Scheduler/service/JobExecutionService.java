package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.repo.*;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobExecutionService {

    private final JobExecutionRepository repository;

    public JobExecutionService(JobExecutionRepository repository) {
        this.repository = repository;
    }

    public JobExecution startExecution(Long jobId, int attempt) {
        JobExecution execution = new JobExecution(
                jobId,
                attempt,
                ExecutionStatus.RUNNING,
                LocalDateTime.now()
        );

        return repository.save(execution);
    }

    public void completeExecution(JobExecution execution) {
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setCompletedAt(LocalDateTime.now());
        repository.save(execution);
    }

    public void failExecution(
            JobExecution execution,
            String errorMessage
    ) {
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setCompletedAt(LocalDateTime.now());
        execution.setErrorMessage(errorMessage);
        repository.save(execution);
    }

    public List<JobExecution> getExecutions(Long jobId) {
        return repository.findByJobIdOrderByStartedAtAsc(jobId);
    }
}