package com.example.Job_Scheduler.repo;

import com.example.Job_Scheduler.model.OutboxEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
