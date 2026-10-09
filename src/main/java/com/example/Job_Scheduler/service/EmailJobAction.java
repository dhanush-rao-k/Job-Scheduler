package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Component
public class EmailJobAction implements JobAction {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public EmailJobAction(
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public void execute(Job job) {

        try {
            EmailPayload emailPayload =
                    objectMapper.readValue(
                            job.getPayload(),
                            EmailPayload.class
                    );

            Set<ConstraintViolation<EmailPayload>> violations =
                    validator.validate(emailPayload);

            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid email payload: " + violations
                );
            }

            // Simulate sending the email for now
            System.out.println(
                    "Sending email to: " + emailPayload.getTo()
            );

            System.out.println(
                    "Subject: " + emailPayload.getSubject()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to execute email job",
                    e
            );
        }
    }
}