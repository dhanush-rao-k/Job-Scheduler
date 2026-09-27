package com.example.Job_Scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatusAndRunAtLessThanEqual(JobStatus status, LocalDateTime time);
}