package com.example.Job_Scheduler.service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(
        name = "outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisher {

    static final String TOPIC = "job-dispatch";
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final OutboxEventClaimService outboxEventClaimService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Duration claimDuration;
    private final Duration sendTimeout;

    public OutboxPublisher(
            OutboxEventClaimService outboxEventClaimService,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${outbox.publisher.claim-duration:PT5M}") Duration claimDuration,
            @Value("${outbox.publisher.send-timeout:PT30S}") Duration sendTimeout
    ) {
        this.outboxEventClaimService = outboxEventClaimService;
        this.kafkaTemplate = kafkaTemplate;
        this.claimDuration = claimDuration;
        this.sendTimeout = sendTimeout;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        publishPendingEvents(DEFAULT_BATCH_SIZE);
    }

    void publishPendingEvents(int batchSize) {
        for (int i = 0; i < batchSize; i++) {
            OutboxEventClaimService.ClaimedEvent claimedEvent =
                    outboxEventClaimService.claimNextEvent(claimDuration);
            if (claimedEvent == null) {
                return;
            }
            publish(claimedEvent);
        }
    }

    private void publish(OutboxEventClaimService.ClaimedEvent claimedEvent) {
        try {
            Future<SendResult<String, String>> result = kafkaTemplate.send(
                    TOPIC,
                    String.valueOf(claimedEvent.aggregateId()),
                    claimedEvent.payload()
            );
            SendResult<String, String> sendResult = result.get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            outboxEventClaimService.markPublished(claimedEvent);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            outboxEventClaimService.markFailed(claimedEvent, exception);
        } catch (ExecutionException | TimeoutException exception) {
            outboxEventClaimService.markFailed(claimedEvent, exception);
        } catch (RuntimeException exception) {
            outboxEventClaimService.markFailed(claimedEvent, exception);
        }
    }

}
