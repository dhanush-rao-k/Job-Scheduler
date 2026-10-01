package com.example.Job_Scheduler;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class BackoffCalculator {

    public long calculateBackoff(int attempt) {
        // Exponential backoff with jitter
        long baseDelay = (long) Math.pow(2, attempt - 1) * 5000L;
        long jitter = ThreadLocalRandom.current().nextLong(0, 5001);
        return baseDelay + jitter;
    }
}