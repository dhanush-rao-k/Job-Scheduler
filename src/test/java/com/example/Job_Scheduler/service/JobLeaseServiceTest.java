package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.repo.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class JobLeaseServiceTest {

    @Mock
    private JobRepository jobRepository;

    private JobLeaseService jobLeaseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jobLeaseService = new JobLeaseService(jobRepository);
    }

    @Test
    void renewLease_shouldExtendLeaseForValidRunningJob() {

        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.RUNNING);
        job.setExecutionToken(UUID.randomUUID());
        job.setLeaseUntil(LocalDateTime.now().plusMinutes(1));

        UUID token = job.getExecutionToken();

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        LocalDateTime oldLease = job.getLeaseUntil();

        boolean renewed =
                jobLeaseService.renewLease(1L, token);

        assertThat(renewed).isTrue();

        assertThat(job.getLeaseUntil())
                .isAfter(oldLease);

        assertThat(job.getLeaseUntil())
                .isAfter(LocalDateTime.now().plusMinutes(4));

        verify(jobRepository)
                .saveAndFlush(job);
    }

    @Test
    void renewLease_shouldRejectWrongExecutionToken() {

        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.RUNNING);
        job.setExecutionToken(UUID.randomUUID());
        job.setLeaseUntil(LocalDateTime.now().plusMinutes(1));

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        boolean renewed =
                jobLeaseService.renewLease(
                        1L,
                        UUID.randomUUID()
                );

        assertThat(renewed).isFalse();
    }

    @Test
    void renewLease_shouldRejectExpiredLease() {

        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.RUNNING);
        job.setExecutionToken(UUID.randomUUID());
        job.setLeaseUntil(LocalDateTime.now().minusSeconds(1));

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        boolean renewed =
                jobLeaseService.renewLease(
                        1L,
                        job.getExecutionToken()
                );

        assertThat(renewed).isFalse();
    }
}