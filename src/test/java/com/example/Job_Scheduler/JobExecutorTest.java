package com.example.Job_Scheduler;

import static org.mockito.Mockito.verify;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThat;

class JobExecutorTest {

    @Mock
    private JobRepository jobRepository;

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
        job.setName("Test Job");
        job.setType("EMAIL");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(LocalDateTime.now());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        jobExecutor.execute(job);

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        verify(jobRepository, times(2)).save(job);
    }
}