package com.example.Job_Scheduler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

@Component
public class HttpJobAction implements JobAction {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final HttpClient httpClient;

    public HttpJobAction(
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
            HttpPayload payload =
                    objectMapper.readValue(
                            job.getPayload(),
                            HttpPayload.class
                    );

            Set<ConstraintViolation<HttpPayload>> violations =
                    validator.validate(payload);

            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid HTTP payload: " + violations
                );
            }

            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder()
                            .uri(URI.create(payload.getUrl()));

            if (payload.getHeaders() != null) {
                payload.getHeaders().forEach(
                        requestBuilder::header
                );
            }

            String body =
                    payload.getBody() == null
                            ? ""
                            : payload.getBody();

            HttpRequest.BodyPublisher bodyPublisher =
                    switch (payload.getMethod()) {
                        case GET, DELETE ->
                                HttpRequest.BodyPublishers.noBody();
                        default ->
                                HttpRequest.BodyPublishers.ofString(body);
                    };

            HttpRequest request =
                    switch (payload.getMethod()) {
                        case GET ->
                                requestBuilder.GET().build();
                        case POST ->
                                requestBuilder.POST(bodyPublisher).build();
                        case PUT ->
                                requestBuilder.PUT(bodyPublisher).build();
                        case PATCH ->
                                requestBuilder.method("PATCH", bodyPublisher).build();
                        case DELETE ->
                                requestBuilder.DELETE().build();
                    };

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                throw new RuntimeException(
                        "HTTP request failed with status "
                                + response.statusCode()
                );
            }

            System.out.println(
                    "HTTP job completed with status "
                            + response.statusCode()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to execute HTTP job",
                    e
            );
        }
    }
}