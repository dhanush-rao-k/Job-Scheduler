package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.Job;
import com.example.Job_Scheduler.model.JobStatus;
import com.example.Job_Scheduler.model.OutboxEvent;
import com.example.Job_Scheduler.model.OutboxEventStatus;
import com.example.Job_Scheduler.repo.JobRepository;
import com.example.Job_Scheduler.repo.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobDispatchService {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String JOB_AGGREGATE_TYPE = "JOB";
    private static final String DISPATCH_REQUESTED_EVENT = "JOB_DISPATCH_REQUESTED";

    private final JobRepository jobRepository;
    private final OutboxEventRepository outboxEventRepository;

    public JobDispatchService(
            JobRepository jobRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.jobRepository = jobRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public int dispatchDueJobs() {
        return dispatchDueJobs(DEFAULT_BATCH_SIZE);
    }

    /**
     * Dispatches due jobs by writing a JOB_DISPATCH_REQUESTED outbox event.
     *
     * The event payload is the documented, intentionally small JSON shape
     * {"jobId":<job id>}; the event aggregate_id contains the same job id.
     */
    @Transactional
    public int dispatchDueJobs(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Job> jobs = jobRepository.findDuePendingJobsForUpdateSkipLocked(now, batchSize);

        for (Job job : jobs) {
            job.setStatus(JobStatus.DISPATCHED);
            job.setUpdatedAt(now);
            jobRepository.saveAndFlush(job);

            OutboxEvent event = new OutboxEvent(
                    JOB_AGGREGATE_TYPE,
                    job.getId(),
                    DISPATCH_REQUESTED_EVENT,
                    "{\"jobId\":" + job.getId() + "}",
                    OutboxEventStatus.PENDING,
                    now
            );
            outboxEventRepository.saveAndFlush(event);
        }

        return jobs.size();
    }
}
