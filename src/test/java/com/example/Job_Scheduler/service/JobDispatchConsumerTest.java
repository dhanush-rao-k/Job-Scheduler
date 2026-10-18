package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.service.JobClaimService.ClaimedJob;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;

import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobDispatchConsumerTest {

    @Mock
    private JobClaimService jobClaimService;

    @Mock
    private JobExecutor jobExecutor;

    @Mock
    private Acknowledgment acknowledgment;

    private JobDispatchConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new JobDispatchConsumer(
                new ObjectMapper(),
                jobClaimService,
                jobExecutor
        );
    }

    @Test
    void validMessageClaimsExecutesThenAcknowledges() {
        UUID token = UUID.randomUUID();
        when(jobClaimService.claimDispatchedJob(42L))
                .thenReturn(Optional.of(new ClaimedJob(42L, token)));

        consumer.consume("{\"jobId\":42}", acknowledgment);

        verify(jobClaimService).claimDispatchedJob(42L);
        verify(jobExecutor).execute(42L, token);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void duplicateOrWrongStatusMessageAcknowledgesWithoutExecution() {
        when(jobClaimService.claimDispatchedJob(42L)).thenReturn(Optional.empty());

        consumer.consume("{\"jobId\":42}", acknowledgment);

        verify(jobExecutor, never()).execute(any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void malformedMessageIsAcknowledgedWithoutClaiming() {
        consumer.consume("{\"jobId\":\"not-a-number\"}", acknowledgment);

        verify(jobClaimService, never()).claimDispatchedJob(any());
        verify(jobExecutor, never()).execute(any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void executionFailureIsNotAcknowledged() {
        UUID token = UUID.randomUUID();
        when(jobClaimService.claimDispatchedJob(42L))
                .thenReturn(Optional.of(new ClaimedJob(42L, token)));
        doThrow(new IllegalStateException("handoff failed"))
                .when(jobExecutor).execute(eq(42L), eq(token));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> consumer.consume("{\"jobId\":42}", acknowledgment)
        ).isInstanceOf(IllegalStateException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
