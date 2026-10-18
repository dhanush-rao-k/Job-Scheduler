package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.OutboxEvent;
import com.example.Job_Scheduler.model.OutboxEventStatus;
import com.example.Job_Scheduler.repo.OutboxEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "outbox.publisher.fixed-delay-ms=600000")
class OutboxPublisherRecoveryIntegrationTest {

    @Autowired
    private OutboxEventClaimService claimService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private java.util.UUID eventId;

    @AfterEach
    void removeEvent() {
        if (eventId != null) {
            outboxEventRepository.deleteById(eventId);
        }
    }

    @Test
    void brokerFailureLeavesEventRetryableAndLaterPublicationSucceeds() {
        OutboxEvent event = outboxEventRepository.saveAndFlush(new OutboxEvent(
                "JOB",
                991L,
                "JOB_DISPATCH_REQUESTED",
                "{\"jobId\":991}",
                OutboxEventStatus.PENDING,
                LocalDateTime.now().minusYears(10)
        ));
        eventId = event.getId();

        OutboxPublisher publisher = new OutboxPublisher(
                claimService,
                kafkaTemplate,
                Duration.ofMinutes(5),
                Duration.ofSeconds(1)
        );

        when(kafkaTemplate.send("job-dispatch", "991", "{\"jobId\":991}"))
                .thenReturn(CompletableFuture.failedFuture(
                        new IllegalStateException("broker unavailable")))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents(1);

        OutboxEvent retryable = outboxEventRepository.findById(eventId).orElseThrow();
        assertThat(retryable.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(retryable.getAttempts()).isEqualTo(1);
        assertThat(retryable.getClaimToken()).isNull();
        assertThat(retryable.getLastError()).contains("broker unavailable");

        publisher.publishPendingEvents(1);

        OutboxEvent published = outboxEventRepository.findById(eventId).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(published.getAttempts()).isEqualTo(1);
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.getClaimToken()).isNull();
    }
}
