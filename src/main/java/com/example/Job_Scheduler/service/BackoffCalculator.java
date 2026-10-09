package com.example.Job_Scheduler.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class BackoffCalculator {

    public long calculateBackoff(int attempt) {
        if (attempt < 1) {
    throw new IllegalArgumentException("Attempt must be at least 1");
    }
        // Exponential backoff with jitter
        long baseDelay = 5000L * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(0, 5001);
        return baseDelay + jitter;
    }
}