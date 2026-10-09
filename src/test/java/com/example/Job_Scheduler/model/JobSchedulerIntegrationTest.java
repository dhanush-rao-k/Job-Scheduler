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
    private JobScheduler jobScheduler;

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
    void scheduler_shouldExecuteDueJob() throws InterruptedException {

        LocalDateTime now = LocalDateTime.now();

        Job job = new Job();
        job.setName("Integration Test Job");
        job.setType("EMAIL");
        job.setStatus(JobStatus.PENDING);
        job.setAttempt(0);
        job.setMaxAttempts(3);
        job.setRunAt(now.minusMinutes(1));
        job.setPayload(
                "{\"to\":[\"user@example.com\"]," +
                "\"subject\":\"Integration Test\"," +
                "\"body\":\"Hello from the scheduler integration test\"}"
        );
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        Job savedJob = jobRepository.save(job);

        when(idempotencyService.isCompleted("job-" + savedJob.getId()))
                .thenReturn(false);

        jobScheduler.findDueJobs();

        Job executedJob = null;

        for (int attempt = 0; attempt < 100; attempt++) {

            entityManager.clear();

            executedJob = jobRepository
                    .findById(savedJob.getId())
                    .orElseThrow();

            if (executedJob.getStatus() == JobStatus.COMPLETED
                    || executedJob.getStatus() == JobStatus.FAILED) {
                break;
            }

            Thread.sleep(10);
        }

        assertThat(executedJob.getStatus())
                .isEqualTo(JobStatus.COMPLETED);

        assertThat(executedJob.getStartedAt())
                .isNotNull();

        assertThat(executedJob.getCompletedAt())
                .isNotNull();

        assertThat(executedJob.getErrorMessage())
                .isNull();

        List<JobExecution> executions =
                jobExecutionRepository
                        .findByJobIdOrderByStartedAtAsc(savedJob.getId());

        assertThat(executions)
                .hasSize(1);

        JobExecution execution = executions.get(0);

        assertThat(execution.getAttempt())
                .isEqualTo(1);

        assertThat(execution.getStatus())
                .isEqualTo(ExecutionStatus.COMPLETED);

        assertThat(execution.getStartedAt())
                .isNotNull();

        assertThat(execution.getCompletedAt())
                .isNotNull();

        assertThat(execution.getErrorMessage())
                .isNull();
    }

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