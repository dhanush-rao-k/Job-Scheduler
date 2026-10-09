package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;
import com.example.Job_Scheduler.repo.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobService jobService;


    @Test
    void createJob_shouldCreatePendingJob() {

        Job job = new Job();
        job.setName("Test Job");
        job.setType("TEST");
        job.setRunAt(LocalDateTime.now());
        job.setPayload("{}");

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Job createdJob = jobService.createJob(job);

        assertNotNull(createdJob);
        assertEquals(JobStatus.PENDING, createdJob.getStatus());
        assertNotNull(createdJob.getCreatedAt());
        assertNotNull(createdJob.getUpdatedAt());

        verify(jobRepository, times(1))
                .save(any(Job.class));
    }


    @Test
    void getJob_shouldReturnJob() {

        Job job = new Job();
        job.setId(1L);
        job.setName("Test Job");
        job.setType("TEST");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(LocalDateTime.now());
        job.setPayload("{}");

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        Job foundJob = jobService.getJob(1L);

        assertNotNull(foundJob);
        assertEquals(1L, foundJob.getId());
        assertEquals("Test Job", foundJob.getName());

        verify(jobRepository, times(1))
                .findById(1L);
    }


    @Test
    void updateJob_shouldUpdateJob() {

        Job oldJob = new Job();
        oldJob.setId(1L);
        oldJob.setName("Test Job");
        oldJob.setType("TEST");
        oldJob.setStatus(JobStatus.PENDING);
        oldJob.setRunAt(LocalDateTime.now());
        oldJob.setPayload("{}");

        Job updatedJobInput = new Job();
        updatedJobInput.setName("Updated Job");
        updatedJobInput.setType("REPORT");
        updatedJobInput.setStatus(JobStatus.PENDING);
        updatedJobInput.setRunAt(LocalDateTime.now());
        updatedJobInput.setPayload("{\"reportType\":\"MONTHLY\"}");

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(oldJob));

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Job updatedJob = jobService.updateJob(1L, updatedJobInput);

        assertNotNull(updatedJob);
        assertEquals(1L, updatedJob.getId());
        assertEquals("Updated Job", updatedJob.getName());
        assertEquals("REPORT", updatedJob.getType());
        assertEquals(JobStatus.PENDING, updatedJob.getStatus());
        assertEquals("{\"reportType\":\"MONTHLY\"}", updatedJob.getPayload());
        assertNotNull(updatedJob.getUpdatedAt());

        verify(jobRepository, times(1))
                .findById(1L);

        verify(jobRepository, times(1))
                .save(any(Job.class));
    }


    @Test
    void updateJob_shouldUpdateStatus() {

        Job oldJob = new Job();
        oldJob.setId(1L);
        oldJob.setName("Test Job");
        oldJob.setType("TEST");
        oldJob.setStatus(JobStatus.PENDING);
        oldJob.setRunAt(LocalDateTime.now());
        oldJob.setPayload("{}");

        Job updatedJobInput = new Job();
        updatedJobInput.setName("Updated Job");
        updatedJobInput.setType("TEST");
        updatedJobInput.setStatus(JobStatus.COMPLETED);
        updatedJobInput.setRunAt(LocalDateTime.now());
        updatedJobInput.setPayload("{}");

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(oldJob));

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Job updatedJob = jobService.updateJob(1L, updatedJobInput);

        assertNotNull(updatedJob);
        assertEquals(JobStatus.COMPLETED, updatedJob.getStatus());

        verify(jobRepository, times(1))
                .findById(1L);

        verify(jobRepository, times(1))
                .save(any(Job.class));
    }


    @Test
    void updateJob_shouldThrowExceptionWhenJobNotFound() {

        when(jobRepository.findById(1L))
                .thenReturn(Optional.empty());

        Job updatedJobInput = new Job();
        updatedJobInput.setName("Updated Job");

        assertThrows(
                RuntimeException.class,
                () -> jobService.updateJob(1L, updatedJobInput)
        );

        verify(jobRepository, times(1))
                .findById(1L);

        verify(jobRepository, never())
                .save(any(Job.class));
    }


    @Test
    void deleteJob_shouldDeleteJob() {

        Job job = new Job();
        job.setId(1L);
        job.setName("Test Job");
        job.setType("TEST");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(LocalDateTime.now());
        job.setPayload("{}");

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        Job deletedJob = jobService.deleteJob(1L);

        assertNotNull(deletedJob);
        assertEquals(1L, deletedJob.getId());
        assertEquals("Test Job", deletedJob.getName());

        verify(jobRepository, times(1))
                .findById(1L);

        verify(jobRepository, times(1))
                .deleteById(1L);
    }


    @Test
    void getAllJobs_shouldReturnAllJobs() {

        Job job1 = new Job();
        job1.setId(1L);
        job1.setName("Test Job 1");
        job1.setType("TEST");
        job1.setStatus(JobStatus.PENDING);
        job1.setRunAt(LocalDateTime.now());
        job1.setPayload("{}");

        Job job2 = new Job();
        job2.setId(2L);
        job2.setName("Test Job 2");
        job2.setType("REPORT");
        job2.setStatus(JobStatus.PENDING);
        job2.setRunAt(LocalDateTime.now());
        job2.setPayload("{\"reportType\":\"MONTHLY\"}");

        List<Job> jobs = new ArrayList<>();
        jobs.add(job1);
        jobs.add(job2);

        when(jobRepository.findAll())
                .thenReturn(jobs);

        List<Job> foundJobs = jobService.getAllJobs();

        assertNotNull(foundJobs);
        assertEquals(2, foundJobs.size());
        assertEquals("Test Job 1", foundJobs.get(0).getName());
        assertEquals("Test Job 2", foundJobs.get(1).getName());

        verify(jobRepository, times(1))
                .findAll();
    }
}