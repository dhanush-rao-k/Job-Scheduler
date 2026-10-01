package com.example.Job_Scheduler;

import com.example.Job_Scheduler.Job;
import com.example.Job_Scheduler.JobStatus;
import com.example.Job_Scheduler.JobRepository;
import com.example.Job_Scheduler.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JobServiceConcurrencyTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void onlyOneWorkerShouldClaimJob() throws Exception {

        // Create a PENDING job
        Job job = new Job();
        job.setName("Concurrent Job");
        job.setType("TEST");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(LocalDateTime.now());
        job.setPayload("{}");
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        Job savedJob = jobRepository.save(job);

        Long jobId = savedJob.getId();

        // Two workers
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Boolean> worker = () -> {
            startLatch.await();
            return jobService.claimJob(jobId);
        };

        Future<Boolean> worker1 = executor.submit(worker);
        Future<Boolean> worker2 = executor.submit(worker);

        // Start both workers at approximately the same time
        startLatch.countDown();

        boolean result1 = worker1.get();
        boolean result2 = worker2.get();

        executor.shutdown();

        // Exactly one worker should successfully claim the job
        assertThat(result1 ^ result2).isTrue();

        Job finalJob = jobRepository.findById(jobId).orElseThrow();

        assertThat(finalJob.getStatus())
                .isEqualTo(JobStatus.RUNNING);
    }
}