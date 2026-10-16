
package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.Job;
import com.example.Job_Scheduler.model.JobAction;
import com.example.Job_Scheduler.model.JobStatus;
import com.example.Job_Scheduler.repo.JobRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@SpringBootTest
class JobExecutorLeaseIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutor jobExecutor;

    @Autowired
    private JobLeaseService jobLeaseService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private JobActionResolver jobActionResolver;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @Test
    void executionShouldCompleteAfterLeaseRenewalChangesJobVersion() {
        LocalDateTime now = LocalDateTime.now();
        UUID token = UUID.randomUUID();

        Job job = new Job();
        job.setName("Lease renewal regression test");
        job.setType("DEFAULT");
        job.setStatus(JobStatus.RUNNING);
        job.setAttempt(0);
        job.setMaxAttempts(3);
        job.setRunAt(now);
        job.setLeaseUntil(now.plusMinutes(5));
        job.setExecutionToken(token);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        Job savedJob = jobRepository.saveAndFlush(job);
        Long jobId = savedJob.getId();

        when(idempotencyService.isCompleted("job-" + jobId))
                .thenReturn(false);

        JobAction action = mock(JobAction.class);
        when(jobActionResolver.resolve("DEFAULT"))
                .thenReturn(action);

        // Renew the real database lease while the executor
        // still holds its original, older Job entity.
        doAnswer(invocation -> {
            assertThat(jobLeaseService.renewLease(jobId, token))
                    .isTrue();
            return null;
        }).when(action).execute(any(Job.class));

        // This should succeed after the production fix.
        assertThatCode(() -> jobExecutor.execute(jobId, token))
                .doesNotThrowAnyException();

        entityManager.clear();

        Job completedJob = jobRepository.findById(jobId)
                .orElseThrow();

        assertThat(completedJob.getStatus())
        .as("Job status; persisted error message: %s",
                completedJob.getErrorMessage())
        .isEqualTo(JobStatus.COMPLETED);
        assertThat(completedJob.getExecutionToken()).isNull();
        assertThat(completedJob.getLeaseUntil()).isNull();
        assertThat(completedJob.getCompletedAt()).isNotNull();
    }
}
