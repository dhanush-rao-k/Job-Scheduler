package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.Job;
import com.example.Job_Scheduler.model.JobStatus;
import com.example.Job_Scheduler.model.OutboxEvent;
import com.example.Job_Scheduler.repo.JobRepository;
import com.example.Job_Scheduler.repo.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class JobDispatchServiceTest {

    @Autowired
    private JobDispatchService jobDispatchService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoSpyBean
    private OutboxEventRepository outboxEventRepositorySpy;

    @BeforeEach
    void clearData() {
        outboxEventRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void dispatchesDuePendingJobAndCreatesRequestEvent() {
        Job job = saveJob(LocalDateTime.now().minusMinutes(1));

        int dispatched = jobDispatchService.dispatchDueJobs();

        entityManager.clear();
        Job reloadedJob = jobRepository.findById(job.getId()).orElseThrow();
        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(dispatched).isEqualTo(1);
        assertThat(reloadedJob.getStatus()).isEqualTo(JobStatus.DISPATCHED);
        assertThat(reloadedJob.getExecutionToken()).isNull();
        assertThat(reloadedJob.getLeaseUntil()).isNull();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getAggregateType()).isEqualTo("JOB");
            assertThat(event.getAggregateId()).isEqualTo(job.getId());
            assertThat(event.getEventType()).isEqualTo("JOB_DISPATCH_REQUESTED");
            assertThat(event.getPayload()).isEqualTo("{\"jobId\":" + job.getId() + "}");
        });
    }

    @Test
    void leavesFutureJobPending() {
        Job job = saveJob(LocalDateTime.now().plusMinutes(1));

        assertThat(jobDispatchService.dispatchDueJobs()).isZero();

        entityManager.clear();
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.PENDING);
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void doesNotRedispatchAfterFirstScan() {
        Job job = saveJob(LocalDateTime.now().minusMinutes(1));

        assertThat(jobDispatchService.dispatchDueJobs()).isEqualTo(1);
        assertThat(jobDispatchService.dispatchDueJobs()).isZero();

        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }

    @Test
    void rollsBackJobTransitionWhenOutboxPersistenceFails() {
        Job job = saveJob(LocalDateTime.now().minusMinutes(1));
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(outboxEventRepositorySpy)
                .saveAndFlush(any(OutboxEvent.class));

        assertThatThrownBy(() -> jobDispatchService.dispatchDueJobs())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox unavailable");

        entityManager.clear();
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.PENDING);
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void dispatchesDueJobOnlyOnceWhenWorkersScanConcurrently() throws Exception {
        Job job = saveJob(LocalDateTime.now().minusMinutes(1));
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Integer> first = workers.submit(() -> {
                start.await();
                return jobDispatchService.dispatchDueJobs();
            });
            Future<Integer> second = workers.submit(() -> {
                start.await();
                return jobDispatchService.dispatchDueJobs();
            });
            start.countDown();

            int firstDispatches = first.get();
            int secondDispatches = second.get();
            assertThat(firstDispatches + secondDispatches).isEqualTo(1);
            assertThat(outboxEventRepository.findAll()).hasSize(1);
            assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus())
                    .isEqualTo(JobStatus.DISPATCHED);
        } finally {
            workers.shutdownNow();
        }
    }

    private Job saveJob(LocalDateTime runAt) {
        LocalDateTime now = LocalDateTime.now();
        Job job = new Job();
        job.setName("Dispatch test job");
        job.setType("TEST");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(runAt);
        job.setPayload("{}");
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return jobRepository.saveAndFlush(job);
    }
}
