package com.example.Job_Scheduler.repo;

import com.example.Job_Scheduler.model.*;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobExecutionRepository
        extends JpaRepository<JobExecution, Long> {

    List<JobExecution> findByJobIdOrderByStartedAtAsc(Long jobId);
}