package com.example.Job_Scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobExecutorTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobActionResolver jobActionResolver;

        @Mock
        private BackoffCalculator backoffCalculator;

    @Mock
    private JobAction jobAction;

    @InjectMocks
    private JobExecutor jobExecutor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_shouldMarkJobCompletedWhenExecutionSucceeds() {
        Job job = new Job();
        job.setId(1L);
        job.setType("EMAIL");
        job.setStatus(JobStatus.RUNNING);
        UUID token = UUID.randomUUID();
        job.setExecutionToken(token);

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        when(jobActionResolver.resolve("EMAIL"))
                .thenReturn(jobAction);

        jobExecutor.execute(1L, token);

        verify(jobActionResolver).resolve("EMAIL");
        verify(jobAction).execute(job);
        verify(jobRepository).save(job);

        assertThat(job.getStatus())
                .isEqualTo(JobStatus.COMPLETED);

        assertThat(job.getCompletedAt())
                .isNotNull();
    }   

    @Test
    void execute_shouldMarkJobFailedWhenExecutionFails() {
        Job job = new Job();
        job.setId(1L);
        job.setType("EMAIL");
        job.setStatus(JobStatus.RUNNING);
        UUID token = UUID.randomUUID();
        job.setExecutionToken(token);

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        when(jobActionResolver.resolve("EMAIL"))
                .thenReturn(jobAction);

        doThrow(new RuntimeException("Execution failed"))
                .when(jobAction)
                .execute(job);

        jobExecutor.execute(1L, token);

        verify(jobActionResolver).resolve("EMAIL");
        verify(jobAction).execute(job);
        verify(jobRepository).save(job);

        assertThat(job.getStatus())
                .isEqualTo(JobStatus.FAILED);

        assertThat(job.getErrorMessage())
                .isEqualTo("Execution failed");

        assertThat(job.getCompletedAt())
                .isNotNull();
    }   

    @Test
void execute_shouldRetryWhenExecutionFailsAndAttemptsRemain() {
    Long jobId = 1L;
    UUID executionToken = UUID.randomUUID();

    Job job = new Job();
    job.setId(jobId);
    job.setType("DEFAULT");
    job.setStatus(JobStatus.RUNNING);
    job.setAttempt(0);
    job.setMaxAttempts(3);
    job.setExecutionToken(executionToken);
    job.setLeaseUntil(LocalDateTime.now().plusMinutes(5));

    when(jobRepository.findById(jobId))
            .thenReturn(Optional.of(job));

    JobAction jobAction = mock(JobAction.class);

    when(jobActionResolver.resolve("DEFAULT"))
            .thenReturn(jobAction);

    when(backoffCalculator.calculateBackoff(1))
            .thenReturn(5500L);

    doThrow(new RuntimeException("execution failed"))
            .when(jobAction)
            .execute(job);

    LocalDateTime before = LocalDateTime.now();

    jobExecutor.execute(jobId, executionToken);

    LocalDateTime after = LocalDateTime.now();

    assertEquals(1, job.getAttempt());
    assertEquals(JobStatus.PENDING, job.getStatus());

    assertTrue(
            job.getRunAt().isAfter(before.plusNanos(5_400_000_000L)) &&
            job.getRunAt().isBefore(after.plusNanos(5_700_000_000L))
    );

    assertEquals("execution failed", job.getErrorMessage());

    assertNull(job.getExecutionToken());
    assertNull(job.getLeaseUntil());

    verify(jobAction).execute(job);
        verify(jobRepository).save(job);
}

@Test
void execute_shouldMarkJobFailedWhenFinalAttemptFails() {
    Long jobId = 1L;
    UUID executionToken = UUID.randomUUID();

    Job job = new Job();
    job.setId(jobId);
    job.setType("DEFAULT");
    job.setStatus(JobStatus.RUNNING);
    job.setAttempt(2);
    job.setMaxAttempts(3);
    job.setExecutionToken(executionToken);
    job.setLeaseUntil(LocalDateTime.now().plusMinutes(5));

    when(jobRepository.findById(jobId))
            .thenReturn(Optional.of(job));

    JobAction jobAction = mock(JobAction.class);

    when(jobActionResolver.resolve("DEFAULT"))
            .thenReturn(jobAction);

    doThrow(new RuntimeException("execution failed"))
            .when(jobAction)
            .execute(job);

    jobExecutor.execute(jobId, executionToken);

    assertEquals(3, job.getAttempt());
    assertEquals(JobStatus.FAILED, job.getStatus());

    assertNotNull(job.getCompletedAt());
    assertEquals("execution failed", job.getErrorMessage());

    assertNull(job.getExecutionToken());
    assertNull(job.getLeaseUntil());

    verify(jobAction).execute(job);
        verify(jobRepository).save(job);
}
}