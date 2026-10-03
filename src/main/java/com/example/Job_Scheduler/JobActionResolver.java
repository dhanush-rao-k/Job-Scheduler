package com.example.Job_Scheduler;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class JobActionResolver {

    private final Map<String, JobAction> actions;

    public JobActionResolver(
            DefaultJobAction defaultJobAction,
            EmailJobAction emailJobAction,
            HttpJobAction httpJobAction,
            WebhookJobAction webhookJobAction
    ) {
        this.actions = Map.of(
                "DEFAULT", defaultJobAction,
                "EMAIL", emailJobAction,
                "HTTP", httpJobAction,
                "WEBHOOK", webhookJobAction
        );
    }

    public JobAction resolve(String type) {
        JobAction action = actions.get(type);

        if (action == null) {
            throw new IllegalArgumentException(
                    "Unsupported job type: " + type
            );
        }

        return action;
    }
}