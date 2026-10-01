package com.example.Job_Scheduler;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class BackoffCalculator {

    public long calculateBackoff(int attempt) {
        // Exponential backoff with jitter
        long baseDelay = (long) Math.pow(2, attempt) * 1000; // Base delay in milliseconds
        long jitter = ThreadLocalRandom.current().nextLong(0, 1000); // Random jitter between 0 and 1 second
        return baseDelay + jitter;
    }
}