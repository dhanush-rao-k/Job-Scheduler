package com.example.Job_Scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Test
    void findByStatusAndRunAtLessThanEqual_shouldReturnPendingJobsReadyToRun() {
        // Setup: Create 3 jobs
        LocalDateTime now = LocalDateTime.now();

        Job job1 = new Job();
        job1.setName("Job 1");
        job1.setType("EMAIL");
        job1.setStatus(JobStatus.PENDING);
        job1.setCreatedAt(now);
        job1.setUpdatedAt(now);
        job1.setRunAt(now.minusHours(1));
        job1.setPayload("{}");
        jobRepository.save(job1);

        Job job2 = new Job();
        job2.setName("Job 2");
        job2.setType("EMAIL");
        job2.setStatus(JobStatus.PENDING);
        job2.setCreatedAt(now);
        job2.setUpdatedAt(now);
        job2.setRunAt(now.plusHours(1));
        job2.setPayload("{}");
        jobRepository.save(job2);

        Job job3 = new Job();
        job3.setName("Job 3");
        job3.setType("SMS");
        job3.setStatus(JobStatus.RUNNING);
        job3.setRunAt(now);
        job3.setCreatedAt(now);
        job3.setUpdatedAt(now);
        job3.setPayload("{}");
        jobRepository.save(job3);

        // Execute
        List<Job> readyJobs = jobRepository.findByStatusAndRunAtLessThanEqual(
                JobStatus.PENDING,
                now
        );

        // Verify
        assertThat(readyJobs).isNotNull();
        assertThat(readyJobs).hasSize(1);
        assertThat(readyJobs.get(0).getId()).isEqualTo(job1.getId());
    }

    @Test
    void findByStatusAndRunAtLessThanEqual_shouldReturnEmptyListWhenNoPendingJobs() {
        LocalDateTime now = LocalDateTime.now();

        Job job = new Job();
        job.setName("Job 1");
        job.setType("EMAIL");
        job.setStatus(JobStatus.RUNNING);
        job.setRunAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setPayload("{}");
        jobRepository.save(job);

        List<Job> readyJobs = jobRepository.findByStatusAndRunAtLessThanEqual(
                JobStatus.PENDING,
                now
        );

        assertThat(readyJobs).isEmpty();
    }

    @Test
    void findByStatusAndRunAtLessThanEqual_shouldReturnEmptyListWhenAllJobsInFuture() {
        LocalDateTime now = LocalDateTime.now();

        Job job = new Job();
        job.setName("Job 1");
        job.setType("EMAIL");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(now.plusHours(1));
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setPayload("{}");
        jobRepository.save(job);

        List<Job> readyJobs = jobRepository.findByStatusAndRunAtLessThanEqual(
                JobStatus.PENDING,
                now
        );

        assertThat(readyJobs).isEmpty();
    }
}
