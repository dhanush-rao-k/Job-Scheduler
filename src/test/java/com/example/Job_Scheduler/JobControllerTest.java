package com.example.Job_Scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class JobControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private JobExecutionService jobExecutionService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createJob_shouldReturnCreatedJob() throws Exception {

        Job job = new Job();

        job.setName("Test Job");
        job.setType("TEST");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        job.setPayload("{}");

        when(jobService.createJob(any(Job.class)))
                .thenReturn(job);

        mockMvc.perform(
                post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(job))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Job"))
        .andExpect(jsonPath("$.type").value("TEST"))
        .andExpect(jsonPath("$.status").value("PENDING"));
    }


    @Test
    void getJob_shouldReturnJob() throws Exception {

        Job job = new Job();

        job.setId(1L);
        job.setName("Test Job");
        job.setType("TEST");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        job.setPayload("{}");

        when(jobService.getJob(1L))
                .thenReturn(job);

        mockMvc.perform(
                get("/api/v1/jobs/1")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Job"))
        .andExpect(jsonPath("$.type").value("TEST"))
        .andExpect(jsonPath("$.status").value("PENDING"));
    }


    @Test
    void getAllJobs_shouldReturnAllJobs() throws Exception {

        Job job1 = new Job();

        job1.setId(1L);
        job1.setName("Test Job 1");
        job1.setType("TEST");
        job1.setStatus(JobStatus.PENDING);
        job1.setRunAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        job1.setPayload("{}");

        Job job2 = new Job();

        job2.setId(2L);
        job2.setName("Test Job 2");
        job2.setType("REPORT");
        job2.setStatus(JobStatus.PENDING);
        job2.setRunAt(LocalDateTime.of(2026, 10, 2, 9, 0));
        job2.setPayload("{}");

        when(jobService.getAllJobs())
                .thenReturn(List.of(job1, job2));

        mockMvc.perform(
                get("/api/v1/jobs")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Test Job 1"))
        .andExpect(jsonPath("$[1].name").value("Test Job 2"));
    }

    @Test
    void getExecutionHistory_shouldReturnHistoryForJob() throws Exception {
        Long jobId = 7L;
        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 1, 9, 15, 0);

        JobExecution execution = new JobExecution(jobId, 1, ExecutionStatus.COMPLETED, startedAt);
        execution.setCompletedAt(startedAt.plusMinutes(2));

        when(jobExecutionService.getExecutions(jobId))
                .thenReturn(List.of(execution));

        mockMvc.perform(
                get("/api/v1/jobs/{jobId}/executions", jobId)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].jobId").value(7))
        .andExpect(jsonPath("$[0].attempt").value(1))
        .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }


    @Test
    void updateJob_shouldReturnUpdatedJob() throws Exception {

        Job updatedJob = new Job();

        updatedJob.setId(1L);
        updatedJob.setName("Updated Job");
        updatedJob.setType("REPORT");
        updatedJob.setStatus(JobStatus.PENDING);
        updatedJob.setRunAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        updatedJob.setPayload("{\"reportType\":\"MONTHLY\"}");

        when(jobService.updateJob(
                any(Long.class),
                any(Job.class)
        )).thenReturn(updatedJob);

        mockMvc.perform(
                put("/api/v1/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedJob))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Updated Job"))
        .andExpect(jsonPath("$.type").value("REPORT"))
        .andExpect(jsonPath("$.status").value("PENDING"));
    }


    @Test
    void deleteJob_shouldReturnDeletedJob() throws Exception {

        Job job = new Job();

        job.setId(1L);
        job.setName("Test Job");
        job.setType("TEST");
        job.setStatus(JobStatus.PENDING);
        job.setRunAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        job.setPayload("{}");

        when(jobService.deleteJob(1L))
                .thenReturn(job);

        mockMvc.perform(
                delete("/api/v1/jobs/1")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Job"));
    }
}