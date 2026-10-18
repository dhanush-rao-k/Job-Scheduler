package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.Job;
import com.example.Job_Scheduler.model.JobStatus;
import com.example.Job_Scheduler.repo.JobRepository;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "outbox.publisher.fixed-delay-ms=600000")
class JobDispatchClaimIntegrationTest {

    @Autowired
    private JobClaimService jobClaimService;

    @Autowired
    private JobRepository jobRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void claimsOnlyDispatchedJobsAndCreatesExecutionOwnership() {
        Job dispatched = saveJob(JobStatus.DISPATCHED);
        Job pending = saveJob(JobStatus.PENDING);

        var claim = jobClaimService.claimDispatchedJob(dispatched.getId()).orElseThrow();

        entityManager.clear();
        Job reloaded = jobRepository.findById(dispatched.getId()).orElseThrow();
        assertThat(claim.jobId()).isEqualTo(dispatched.getId());
        assertThat(reloaded.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(reloaded.getExecutionToken()).isEqualTo(claim.executionToken());
        assertThat(reloaded.getStartedAt()).isNotNull();
        assertThat(reloaded.getLeaseUntil()).isAfter(LocalDateTime.now());
        assertThat(jobClaimService.claimDispatchedJob(pending.getId())).isEmpty();
    }

    @Test
    void duplicateDispatchCannotClaimRunningJobAgain() {
        Job dispatched = saveJob(JobStatus.DISPATCHED);

        assertThat(jobClaimService.claimDispatchedJob(dispatched.getId())).isPresent();
        assertThat(jobClaimService.claimDispatchedJob(dispatched.getId())).isEmpty();
    }

    private Job saveJob(JobStatus status) {
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job();
        job.setName("Dispatch claim test");
        job.setType("TEST");
        job.setStatus(status);
        job.setAttempt(0);
        job.setMaxAttempts(3);
        job.setRunAt(now);
        job.setPayload("{}");
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return jobRepository.saveAndFlush(job);
    }
}
