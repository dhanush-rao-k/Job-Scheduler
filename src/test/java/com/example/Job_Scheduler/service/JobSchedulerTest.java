package com.example.Job_Scheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

class JobSchedulerTest {

    @Mock
    private JobDispatchService jobDispatchService;

    @Mock
    private JobRecoveryService jobRecoveryService;

    @InjectMocks
    private JobScheduler jobScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void dispatchJobsRecoversLeasesBeforeDispatchingDueJobs() {
        jobScheduler.dispatchJobs();

        verify(jobRecoveryService).recoverExpiredJobs();
        verify(jobDispatchService).dispatchDueJobs();
    }
}
