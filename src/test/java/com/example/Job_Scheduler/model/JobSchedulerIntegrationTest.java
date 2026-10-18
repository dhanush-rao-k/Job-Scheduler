package com.example.Job_Scheduler.model;

import com.example.Job_Scheduler.repo.JobExecutionRepository;
import com.example.Job_Scheduler.repo.JobRepository;
import com.example.Job_Scheduler.service.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class JobSchedulerIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private JobRecoveryService jobRecoveryService;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void recovery_shouldResetExpiredRunningJobToPending() {

        LocalDateTime now = LocalDateTime.now();

        Job job = new Job();
        job.setName("Expired Lease Test");
        job.setType("DEFAULT");
        job.setStatus(JobStatus.RUNNING);
        job.setAttempt(1);
        job.setMaxAttempts(3);
        job.setRunAt(now.minusMinutes(5));
        job.setLeaseUntil(now.minusMinutes(1));
        job.setExecutionToken(UUID.randomUUID());
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        Job savedJob = jobRepository.saveAndFlush(job);

        jobRecoveryService.recoverExpiredJobs();

        entityManager.clear();

        Job recoveredJob = jobRepository
                .findById(savedJob.getId())
                .orElseThrow();

        assertThat(recoveredJob.getStatus())
                .isEqualTo(JobStatus.PENDING);

        assertThat(recoveredJob.getLeaseUntil())
                .isNull();

        assertThat(recoveredJob.getExecutionToken())
                .isNull();

        assertThat(recoveredJob.getUpdatedAt())
                .isNotNull();
    }
}