package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.repo.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public boolean isCompleted(String key) {
        return repository.findByKey(key).isPresent();
    }

    public void complete(String key) {
        if (repository.findByKey(key).isPresent()) {
            return;
        }

        IdempotencyRecord record = new IdempotencyRecord(key, java.time.LocalDateTime.now());
        try {
            repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            return;
        }
    }
}