package com.example.Job_Scheduler;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WebhookJobActionTest {

    private WebhookJobAction webhookJobAction;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        Validator validator =
                Validation.buildDefaultValidatorFactory()
                        .getValidator();

        webhookJobAction =
                new WebhookJobAction(
                        objectMapper,
                        validator
                );
    }

    @Test
    void missingUrl_shouldFail() {

        Job job = new Job();

        job.setPayload("""
                {
                  "event": "USER_CREATED",
                  "payload": {
                    "userId": 42
                  }
                }
                """);

        assertThrows(
                RuntimeException.class,
                () -> webhookJobAction.execute(job)
        );
    }

    @Test
    void missingEvent_shouldFail() {

        Job job = new Job();

        job.setPayload("""
                {
                  "url": "https://example.com/webhook",
                  "payload": {
                    "userId": 42
                  }
                }
                """);

        assertThrows(
                RuntimeException.class,
                () -> webhookJobAction.execute(job)
        );
    }

    @Test
    void missingPayload_shouldFail() {

        Job job = new Job();

        job.setPayload("""
                {
                  "url": "https://example.com/webhook",
                  "event": "USER_CREATED"
                }
                """);

        assertThrows(
                RuntimeException.class,
                () -> webhookJobAction.execute(job)
        );
    }

    @Test
    void malformedJson_shouldFail() {

        Job job = new Job();

        job.setPayload("""
                {
                  "url":
                }
                """);

        assertThrows(
                RuntimeException.class,
                () -> webhookJobAction.execute(job)
        );
    }
}