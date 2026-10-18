package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.service.JobClaimService.ClaimedJob;

import java.util.Optional;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class JobDispatchConsumer {

    private final ObjectMapper objectMapper;
    private final JobClaimService jobClaimService;
    private final JobExecutor jobExecutor;

    public JobDispatchConsumer(
            ObjectMapper objectMapper,
            JobClaimService jobClaimService,
            JobExecutor jobExecutor
    ) {
        this.objectMapper = objectMapper;
        this.jobClaimService = jobClaimService;
        this.jobExecutor = jobExecutor;
    }

    @KafkaListener(
            topics = "job-dispatch",
            groupId = "${spring.kafka.consumer.group-id:job-scheduler}",
            concurrency = "${spring.kafka.listener.concurrency:2}"
    )
    public void consume(String payload, Acknowledgment acknowledgment) {
        Long jobId = parseJobId(payload);
        if (jobId == null) {
            acknowledgment.acknowledge();
            return;
        }

        Optional<ClaimedJob> claimedJob = jobClaimService.claimDispatchedJob(jobId);
        if (claimedJob.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }

        ClaimedJob job = claimedJob.get();
        jobExecutor.execute(job.jobId(), job.executionToken());
        acknowledgment.acknowledge();
    }

    private Long parseJobId(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode jobId = root == null ? null : root.get("jobId");
            if (jobId == null || !jobId.isIntegralNumber() || jobId.asLong() <= 0) {
                return null;
            }
            return jobId.asLong();
        } catch (Exception exception) {
            return null;
        }
    }
}
