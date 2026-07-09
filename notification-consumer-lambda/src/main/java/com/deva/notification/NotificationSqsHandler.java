package com.deva.notification;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

public class NotificationSqsHandler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Reused across warm invocations
    private static final SesClient sesClient = SesClient.builder()
            .region(Region.US_EAST_1)
            .build();

// comes from environment variable set in lambda
    private static final String FROM_EMAIL = System.getenv("FROM_EMAIL");

    @Override
    public Void handleRequest(SQSEvent event, Context context) {

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {

                // SQS body contains SNS envelope
                JsonNode snsEnvelope = objectMapper.readTree(message.getBody());

                // Actual payload published by SNS
                String innerMessage = snsEnvelope.get("Message").asText();

                JsonNode payload = objectMapper.readTree(innerMessage);

                String orderId = payload.get("orderId").asText();
                String email = payload.get("email").asText();

                sendEmail(orderId, email);

            } catch (Exception e) {
                throw new RuntimeException("Failed processing SQS message", e);
            }
        }

        return null;
    }

    private void sendEmail(String orderId, String email) {

        SendEmailRequest request = SendEmailRequest.builder()
                .source(FROM_EMAIL)
                .destination(Destination.builder()
                        .toAddresses(email)
                        .build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .data("Order Confirmation")
                                .build())
                        .body(Body.builder()
                                .text(Content.builder()
                                        .data("Your payment was successful.\n\nOrder ID: " + orderId)
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }
}