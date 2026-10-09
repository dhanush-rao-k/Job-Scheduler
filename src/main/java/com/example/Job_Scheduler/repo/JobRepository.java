package com.example.Job_Scheduler.repo;

import com.example.Job_Scheduler.model.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatusAndRunAtLessThanEqual(JobStatus status, LocalDateTime time);
    List<Job> findByStatusAndLeaseUntilBefore(JobStatus status,LocalDateTime time);
}