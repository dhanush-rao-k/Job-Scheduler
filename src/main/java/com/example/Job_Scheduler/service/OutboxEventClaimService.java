package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.OutboxEvent;
import com.example.Job_Scheduler.model.OutboxEventStatus;
import com.example.Job_Scheduler.repo.OutboxEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventClaimService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventClaimService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimedEvent claimNextEvent(Duration claimDuration) {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event = outboxEventRepository.findNextClaimable(now).orElse(null);
        if (event == null) {
            return null;
        }

        UUID token = UUID.randomUUID();
        event.setClaimToken(token);
        event.setClaimUntil(now.plus(claimDuration));
        return new ClaimedEvent(event.getId(), event.getAggregateId(), event.getPayload(), token);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(ClaimedEvent claimedEvent) {
        outboxEventRepository.findById(claimedEvent.id())
                .filter(event -> event.getStatus() == OutboxEventStatus.PENDING)
                .filter(event -> claimedEvent.token().equals(event.getClaimToken()))
                .ifPresent(event -> {
                    event.setStatus(OutboxEventStatus.PUBLISHED);
                    event.setPublishedAt(LocalDateTime.now());
                    event.setClaimToken(null);
                    event.setClaimUntil(null);
                    event.setLastError(null);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(ClaimedEvent claimedEvent, Exception exception) {
        outboxEventRepository.findById(claimedEvent.id())
                .filter(event -> event.getStatus() == OutboxEventStatus.PENDING)
                .filter(event -> claimedEvent.token().equals(event.getClaimToken()))
                .ifPresent(event -> {
                    event.setAttempts(event.getAttempts() + 1);
                    event.setLastError(errorMessage(exception));
                    event.setClaimToken(null);
                    event.setClaimUntil(null);
                });
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    public record ClaimedEvent(UUID id, Long aggregateId, String payload, UUID token) {
    }
}
