package com.example.Job_Scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class JobSchedulerIntegrationTest {

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobRepository jobRepository;

        @MockitoBean
        private IdempotencyService idempotencyService;

        @PersistenceContext
        private EntityManager entityManager;

    @Test
        void scheduler_shouldExecuteDueJob() throws InterruptedException {

        LocalDateTime now = LocalDateTime.now();

        Job job = new Job();
        job.setName("Integration Test Job");
        job.setType("EMAIL");
        job.setStatus(JobStatus.PENDING);
        job.setAttempt(0);
        job.setMaxAttempts(3);
        job.setRunAt(now.minusMinutes(1));
        job.setPayload("{\"to\":[\"user@example.com\"],\"subject\":\"Integration Test\",\"body\":\"Hello from the scheduler integration test\"}");
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        Job savedJob = jobRepository.save(job);
        when(idempotencyService.isCompleted("job-" + savedJob.getId()))
                .thenReturn(false);

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