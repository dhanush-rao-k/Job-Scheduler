package com.example.Job_Scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.annotation.PreDestroy;

class JobSchedulerTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobService jobService;

    @Mock
    private JobExecutor jobExecutor;

    @InjectMocks
    private JobScheduler jobScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @PreDestroy
    void tearDown() {
        jobScheduler.shutdown();
    }

    @Test
    void findDueJobs_shouldSendDueJobsToExecutor() {

        LocalDateTime now = LocalDateTime.now();

        Job job = new Job();
        job.setId(1L);
        job.setName("Test Job");
        job.setType("EMAIL");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(now.minusMinutes(1));
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        when(jobRepository.findByStatusAndRunAtLessThanEqual(
            eq(JobStatus.PENDING),
            any(LocalDateTime.class)))
            .thenReturn(List.of(job));
        when(jobService.claimJob(job.getId())).thenReturn(true);

        jobScheduler.findDueJobs();

        verify(jobExecutor, timeout(1000)).execute(job.getId());
    }

    @Test
    void findDueJobs_shouldNotExecuteAnythingWhenNoJobsAreDue() {

        when(jobRepository.findByStatusAndRunAtLessThanEqual(
                eq(JobStatus.PENDING),
                any(LocalDateTime.class)))
                .thenReturn(List.of());

        jobScheduler.findDueJobs();

        verifyNoInteractions(jobExecutor);
    }
}

