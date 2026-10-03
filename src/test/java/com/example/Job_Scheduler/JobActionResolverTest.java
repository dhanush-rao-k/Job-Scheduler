package com.example.Job_Scheduler;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobActionResolverTest {

    @Mock
    private DefaultJobAction defaultJobAction;

    @Mock
    private EmailJobAction emailJobAction;

    @Mock
    private HttpJobAction httpJobAction;

    @Test
    void resolve_shouldReturnHttpActionForHttpType() {

        JobActionResolver resolver =
                new JobActionResolver(
                        defaultJobAction,
                        emailJobAction,
                        httpJobAction
                );

        assertSame(
                httpJobAction,
                resolver.resolve("HTTP")
        );
    }
}