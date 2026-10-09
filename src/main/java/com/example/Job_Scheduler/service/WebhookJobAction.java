package com.example.Job_Scheduler.service;

import com.example.Job_Scheduler.model.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;

@Component
public class WebhookJobAction implements JobAction {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final HttpClient httpClient;

    public WebhookJobAction(
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void execute(Job job) {
        try {
            WebhookPayload webhookPayload =
                    objectMapper.readValue(
                            job.getPayload(),
                            WebhookPayload.class
                    );

            Set<ConstraintViolation<WebhookPayload>> violations =
                    validator.validate(webhookPayload);

            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid webhook payload: " + violations
                );
            }

            Map<String, Object> requestBody = Map.of(
                    "event", webhookPayload.getEvent(),
                    "payload", webhookPayload.getPayload()
            );

            String jsonBody =
                    objectMapper.writeValueAsString(requestBody);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(webhookPayload.getUrl()))
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            jsonBody
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Webhook failed with status "
                                + response.statusCode()
                );
            }

            System.out.println(
                    "Webhook delivered successfully with status "
                            + response.statusCode()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to execute webhook",
                    e
            );
        }
    }
}