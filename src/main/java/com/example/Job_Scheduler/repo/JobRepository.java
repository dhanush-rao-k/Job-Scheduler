package com.example.Job_Scheduler.repo;

import com.example.Job_Scheduler.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatusAndRunAtLessThanEqual(JobStatus status, LocalDateTime time);
    List<Job> findByStatusAndLeaseUntilBefore(JobStatus status,LocalDateTime time);
    @Modifying
@Query("""
    UPDATE Job j
    SET j.status = :status,
        j.attempt = :attempt,
        j.runAt = :runAt,
        j.completedAt = :completedAt,
        j.errorMessage = :errorMessage,
        j.executionToken = null,
        j.leaseUntil = null,
        j.updatedAt = :updatedAt,
        j.version = j.version + 1
    WHERE j.id = :jobId
      AND j.executionToken = :executionToken
      AND j.status = :expectedStatus
    """)
    @Transactional
int finishOwnedExecution(
        @Param("jobId") Long jobId,
        @Param("executionToken") UUID executionToken,
        @Param("expectedStatus") JobStatus expectedStatus,
        @Param("status") JobStatus status,
        @Param("attempt") int attempt,
        @Param("runAt") LocalDateTime runAt,
        @Param("completedAt") LocalDateTime completedAt,
        @Param("errorMessage") String errorMessage,
        @Param("updatedAt") LocalDateTime updatedAt
);
}
