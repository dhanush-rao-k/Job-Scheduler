package com.example.Job_Scheduler.service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventClaimService claimService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(
                claimService,
                kafkaTemplate,
                Duration.ofMinutes(5),
                Duration.ofSeconds(1)
        );
    }

    @Test
    void publishesAndMarksOnlyAfterKafkaAcknowledges() {
        OutboxEventClaimService.ClaimedEvent claimed = claimedEvent();
        when(claimService.claimNextEvent(any())).thenReturn(claimed).thenReturn(null);
        when(kafkaTemplate.send("job-dispatch", "42", "{\"jobId\":42}"))
                .thenReturn(acknowledged());

        publisher.publishPendingEvents(10);

        verify(kafkaTemplate).send("job-dispatch", "42", "{\"jobId\":42}");
        verify(claimService).markPublished(claimed);
        verify(claimService, org.mockito.Mockito.times(2))
                .claimNextEvent(Duration.ofMinutes(5));
    }

    @Test
    void recordsKafkaFailureAsRetryable() {
        OutboxEventClaimService.ClaimedEvent claimed = claimedEvent();
        RuntimeException failure = new RuntimeException("broker unavailable");
        when(claimService.claimNextEvent(any())).thenReturn(claimed).thenReturn(null);
        when(kafkaTemplate.send(eq("job-dispatch"), eq("42"), eq("{\"jobId\":42}")))
                .thenReturn(CompletableFuture.failedFuture(failure));

        publisher.publishPendingEvents(1);

        verify(claimService).markFailed(eq(claimed), any(Exception.class));
        verifyNoInteractionsWithPublished();
    }

    @Test
    void stopsWhenThereAreNoClaimableEvents() {
        when(claimService.claimNextEvent(any())).thenReturn(null);

        publisher.publishPendingEvents(10);

        verifyNoInteractions(kafkaTemplate);
    }

    private OutboxEventClaimService.ClaimedEvent claimedEvent() {
        return new OutboxEventClaimService.ClaimedEvent(
                UUID.randomUUID(), 42L, "{\"jobId\":42}", UUID.randomUUID());
    }

    private CompletableFuture<SendResult<String, String>> acknowledged() {
        CompletableFuture<SendResult<String, String>> result = new CompletableFuture<>();
        result.complete(null);
        return result;
    }

    private void verifyNoInteractionsWithPublished() {
        org.mockito.Mockito.verify(claimService, org.mockito.Mockito.never())
                .markPublished(any());
    }
}
