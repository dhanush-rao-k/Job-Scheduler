
package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.Job;
import com.example.Job_Scheduler.model.JobStatus;
import com.example.Job_Scheduler.repo.JobRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JobRecoveryServiceTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobRecoveryService jobRecoveryService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void recoverExpiredJobs_shouldReturnJobToPendingAndClearOwnership() {
        LocalDateTime now = LocalDateTime.now();

        Job job = new Job();
        job.setName("Expired lease recovery test");
        job.setType("TEST");
        job.setStatus(JobStatus.RUNNING);
        job.setRunAt(now.minusMinutes(10));
        job.setLeaseUntil(now.minusMinutes(1));
        job.setExecutionToken(UUID.randomUUID());
        job.setPayload("{}");
        job.setCreatedAt(now.minusMinutes(10));
        job.setUpdatedAt(now.minusMinutes(5));

        Long jobId = jobRepository.saveAndFlush(job).getId();

        jobRecoveryService.recoverExpiredJobs();

        entityManager.clear();

        Job recovered = jobRepository.findById(jobId).orElseThrow();

        assertThat(recovered.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(recovered.getExecutionToken()).isNull();
        assertThat(recovered.getLeaseUntil()).isNull();
        assertThat(recovered.getUpdatedAt()).isAfter(now.minusMinutes(1));
    }
}
