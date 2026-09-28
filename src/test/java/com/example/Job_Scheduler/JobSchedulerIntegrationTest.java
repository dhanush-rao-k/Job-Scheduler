package com.example.Job_Scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class JobSchedulerIntegrationTest {

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void scheduler_shouldExecuteDueJob() {

        LocalDateTime now = LocalDateTime.now();

        Job job = new Job();
        job.setName("Integration Test Job");
        job.setType("EMAIL");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(now.minusMinutes(1));
        job.setPayload("{}");
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        Job savedJob = jobRepository.save(job);

        jobScheduler.findDueJobs();

        Job executedJob = jobRepository
                .findById(savedJob.getId())
                .orElseThrow();

        assertThat(executedJob.getStatus())
                .isEqualTo(JobStatus.COMPLETED);

        assertThat(executedJob.getStartedAt())
                .isNotNull();

        assertThat(executedJob.getCompletedAt())
                .isNotNull();

        assertThat(executedJob.getErrorMessage())
                .isNull();
    }
}