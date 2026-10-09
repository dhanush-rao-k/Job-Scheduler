package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpJobActionTest {

    private HttpJobAction httpJobAction;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        Validator validator =
                Validation.buildDefaultValidatorFactory()
                        .getValidator();

        httpJobAction =
                new HttpJobAction(
                        objectMapper,
                        validator
                );
    }

    @Test
    void missingUrl_shouldFail() {

        Job job = new Job();

        job.setPayload("""
                {
                  "method": "GET"
                }
                """);

        assertThrows(
                RuntimeException.class,
                () -> httpJobAction.execute(job)
        );
    }

    @Test
    void missingMethod_shouldFail() {

        Job job = new Job();

        job.setPayload("""
                {
                  "url": "https://example.com"
                }
                """);

        assertThrows(
                RuntimeException.class,
                () -> httpJobAction.execute(job)
        );
    }

    @Test
    void malformedJson_shouldFail() {

        Job job = new Job();

        job.setPayload("""
                {
                  "url": "https://example.com",
                  "method":
                }
                """);

        assertThrows(
                RuntimeException.class,
                () -> httpJobAction.execute(job)
        );
    }
}