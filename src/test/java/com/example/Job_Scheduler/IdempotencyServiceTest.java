package com.example.Job_Scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IdempotencyServiceTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRecordRepository repository;

    @Test
    void concurrentCompletion_shouldCreateOnlyOneRecord() throws Exception {
        String key = "concurrent-test-" + System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<?> first = executor.submit(() -> {
            await(start);
            idempotencyService.complete(key);
        });

        Future<?> second = executor.submit(() -> {
            await(start);
            idempotencyService.complete(key);
        });

        start.countDown();

        first.get();
        second.get();

        executor.shutdown();

        List<IdempotencyRecord> records = repository.findAll()
                .stream()
                .filter(record -> record.getKey().equals(key))
                .toList();

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getKey()).isEqualTo(key);
        assertThat(records.get(0).getCompletedAt()).isNotNull();
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}