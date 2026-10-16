package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.annotation.PreDestroy;

class JobSchedulerTest {

    @Mock
    private JobService jobService;

    @Mock
    private JobExecutor jobExecutor;

    @Mock
    private JobRecoveryService jobRecoveryService;

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

        Long jobId = 1L;
        UUID token = UUID.randomUUID();
        when(jobService.claimNextJob())
                .thenReturn(new JobClaimService.ClaimedJob(jobId, token))
                .thenReturn(null);

        jobScheduler.findDueJobs();

        verify(jobRecoveryService).recoverExpiredJobs();
        verify(jobExecutor, timeout(1000)).execute(jobId, token);
    }

    @Test
    void findDueJobs_shouldNotExecuteAnythingWhenNoJobsAreDue() {

        when(jobService.claimNextJob()).thenReturn(null);

        jobScheduler.findDueJobs();

        verify(jobRecoveryService).recoverExpiredJobs();
        verifyNoInteractions(jobExecutor);
    }
}

