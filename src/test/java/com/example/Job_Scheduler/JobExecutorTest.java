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
import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThat;

class JobExecutorTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobActionResolver jobActionResolver;

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
}