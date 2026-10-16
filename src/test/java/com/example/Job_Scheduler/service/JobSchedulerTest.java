package com.example.Job_Scheduler.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;
import java.util.UUID;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    
    @Test
    void findDueJobs_shouldNotClaimMoreJobsThanAvailableSlots()
            throws InterruptedException {

        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        when(jobService.claimNextJob())
                .thenReturn(new JobClaimService.ClaimedJob(1L, UUID.randomUUID()))
                .thenReturn(new JobClaimService.ClaimedJob(2L, UUID.randomUUID()))
                .thenReturn(new JobClaimService.ClaimedJob(3L, UUID.randomUUID()));

        doAnswer(invocation -> {
            started.countDown();
            release.await(3, TimeUnit.SECONDS);
            return null;
        }).when(jobExecutor).execute(any(Long.class), any(UUID.class));

        try {
            jobScheduler.findDueJobs();

            assertTrue(
                    started.await(2, TimeUnit.SECONDS),
                    "Both execution slots should start"
            );

            verify(jobService, times(2)).claimNextJob();
        } finally {
            release.countDown();
        }
    }

}

