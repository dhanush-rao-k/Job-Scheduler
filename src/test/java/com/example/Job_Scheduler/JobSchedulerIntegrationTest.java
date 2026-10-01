package com.example.Job_Scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class JobSchedulerIntegrationTest {

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobRepository jobRepository;

        @PersistenceContext
        private EntityManager entityManager;

    @Test
        void scheduler_shouldExecuteDueJob() throws InterruptedException {

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

        Job executedJob = null;
        for (int attempt = 0; attempt < 100; attempt++) {
                        entityManager.clear();
            executedJob = jobRepository
                    .findById(savedJob.getId())
                    .orElseThrow();
            if (executedJob.getStatus() == JobStatus.COMPLETED
                    || executedJob.getStatus() == JobStatus.FAILED) {
                break;
            }
            Thread.sleep(10);
        }

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