package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.OutboxEvent;
import com.example.Job_Scheduler.model.OutboxEventStatus;
import com.example.Job_Scheduler.repo.OutboxEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "outbox.publisher.fixed-delay-ms=600000")
class OutboxEventClaimServiceConcurrencyTest {

    @Autowired
    private OutboxEventClaimService claimService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void clearData() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void concurrentPublishersClaimDifferentEvents() throws Exception {
        outboxEventRepository.saveAllAndFlush(List.of(event(1L), event(2L)));
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<OutboxEventClaimService.ClaimedEvent> first = workers.submit(() -> {
                start.await();
                return claimService.claimNextEvent(Duration.ofMinutes(5));
            });
            Future<OutboxEventClaimService.ClaimedEvent> second = workers.submit(() -> {
                start.await();
                return claimService.claimNextEvent(Duration.ofMinutes(5));
            });
            start.countDown();

            OutboxEventClaimService.ClaimedEvent firstClaim = first.get();
            OutboxEventClaimService.ClaimedEvent secondClaim = second.get();

            assertThat(firstClaim).isNotNull();
            assertThat(secondClaim).isNotNull();
            assertThat(firstClaim.id()).isNotEqualTo(secondClaim.id());
            assertThat(outboxEventRepository.findAll())
                    .allSatisfy(event -> assertThat(event.getClaimToken()).isNotNull());
        } finally {
            workers.shutdownNow();
        }
    }

    private OutboxEvent event(Long aggregateId) {
        return new OutboxEvent(
                "JOB",
                aggregateId,
                "JOB_DISPATCH_REQUESTED",
                "{\"jobId\":" + aggregateId + "}",
                OutboxEventStatus.PENDING,
                LocalDateTime.now()
        );
    }
}
