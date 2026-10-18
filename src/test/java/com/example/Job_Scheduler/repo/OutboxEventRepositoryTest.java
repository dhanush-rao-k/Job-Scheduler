package com.example.Job_Scheduler.repo;

import com.example.Job_Scheduler.model.OutboxEvent;
import com.example.Job_Scheduler.model.OutboxEventStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndReloadsOutboxEvent() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 18, 14, 0);
        Long aggregateId = 42L;
        String payload = "{\"jobId\":42}";

        OutboxEvent event = new OutboxEvent(
                "JOB",
                aggregateId,
                "JOB_DISPATCH_REQUESTED",
                payload,
                OutboxEventStatus.PENDING,
                createdAt
        );

        OutboxEvent savedEvent = outboxEventRepository.saveAndFlush(event);
        entityManager.clear();

        OutboxEvent reloadedEvent = outboxEventRepository
                .findById(savedEvent.getId())
                .orElseThrow();

        assertThat(reloadedEvent.getId()).isEqualTo(savedEvent.getId());
        assertThat(reloadedEvent.getEventType()).isEqualTo("JOB_DISPATCH_REQUESTED");
        assertThat(reloadedEvent.getAggregateId()).isEqualTo(aggregateId);
        assertThat(reloadedEvent.getPayload()).isEqualTo(payload);
        assertThat(reloadedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(reloadedEvent.getAttempts()).isZero();
        assertThat(reloadedEvent.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloadedEvent.getPublishedAt()).isNull();
        assertThat(reloadedEvent.getLastError()).isNull();
    }
}
