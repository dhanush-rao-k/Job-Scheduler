package com.example.Job_Scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceTest {

    @Mock
    private JobExecutionRepository repository;

    @InjectMocks
    private JobExecutionService service;

    @Test
    void startExecution_shouldCreateRunningExecution() {
        when(repository.save(any(JobExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobExecution execution =
                service.startExecution(1L, 2);

        assertEquals(1L, execution.getJobId());
        assertEquals(2, execution.getAttempt());
        assertEquals(
                ExecutionStatus.RUNNING,
                execution.getStatus()
        );

        verify(repository).save(execution);
    }

    @Test
    void completeExecution_shouldMarkExecutionCompleted() {
        JobExecution execution =
                new JobExecution(
                        1L,
                        1,
                        ExecutionStatus.RUNNING,
                        LocalDateTime.now()
                );

        service.completeExecution(execution);

        assertEquals(
                ExecutionStatus.COMPLETED,
                execution.getStatus()
        );

        verify(repository).save(execution);
    }

    @Test
    void failExecution_shouldMarkExecutionFailed() {
        JobExecution execution =
                new JobExecution(
                        1L,
                        1,
                        ExecutionStatus.RUNNING,
                        LocalDateTime.now()
                );

        service.failExecution(
                execution,
                "Connection refused"
        );

        assertEquals(
                ExecutionStatus.FAILED,
                execution.getStatus()
        );

        assertEquals(
                "Connection refused",
                execution.getErrorMessage()
        );

        verify(repository).save(execution);
    }

    @Test
    void getExecutions_shouldReturnJobHistory() {
        List<JobExecution> executions = List.of(
                new JobExecution(
                        1L,
                        1,
                        ExecutionStatus.FAILED,
                        LocalDateTime.now()
                ),
                new JobExecution(
                        1L,
                        2,
                        ExecutionStatus.COMPLETED,
                        LocalDateTime.now()
                )
        );

        when(repository.findByJobIdOrderByStartedAtAsc(1L))
                .thenReturn(executions);

        assertEquals(
                executions,
                service.getExecutions(1L)
        );

        verify(repository)
                .findByJobIdOrderByStartedAtAsc(1L);
    }
}