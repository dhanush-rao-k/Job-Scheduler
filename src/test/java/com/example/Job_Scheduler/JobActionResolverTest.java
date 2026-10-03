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

    @Mock
    private WebhookJobAction webhookJobAction;

    @Test
    void resolve_shouldReturnHttpActionForHttpType() {

        JobActionResolver resolver =
                new JobActionResolver(
                        defaultJobAction,
                        emailJobAction,
                        httpJobAction,
                        webhookJobAction
                );

        assertSame(
                httpJobAction,
                resolver.resolve("HTTP")
        );
    }
    @Test
void resolve_shouldReturnWebhookActionForWebhookType() {

    JobActionResolver resolver =
            new JobActionResolver(
                    defaultJobAction,
                    emailJobAction,
                    httpJobAction,
                    webhookJobAction
            );

    assertSame(
            webhookJobAction,
            resolver.resolve("WEBHOOK")
    );
}
    @Test
    void resolve_shouldReturnDefaultActionForDefaultType() {

        JobActionResolver resolver =
                new JobActionResolver(
                        defaultJobAction,
                        emailJobAction,
                        httpJobAction,
                        webhookJobAction
                );

        assertSame(
                defaultJobAction,
                resolver.resolve("DEFAULT")
        );
    }

    @Test
    void resolve_shouldReturnEmailActionForEmailType() {

        JobActionResolver resolver =
                new JobActionResolver(
                        defaultJobAction,
                        emailJobAction,
                        httpJobAction,
                        webhookJobAction
                );

        assertSame(
                emailJobAction,
                resolver.resolve("EMAIL")
        );
    }
}