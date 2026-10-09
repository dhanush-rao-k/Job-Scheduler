package com.example.Job_Scheduler.repo;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.service.IdempotencyRecord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByKey(String key);
}