package com.deva.orderservice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.deva.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;

public class PaymentSuccessSqsHandler implements RequestHandler<SQSEvent, Void> {

    private static final ConfigurableApplicationContext context =
            SpringApplication.run(OrderServiceApplication.class);

    private final OrderService orderService = context.getBean(OrderService.class);
    private final ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

    @Override
    public Void handleRequest(SQSEvent event, Context lambdaContext) {
        Subsegment subsegment = AWSXRay.beginSubsegment("handlePostPaymentSuccess");
        try {
            if (event != null && event.getRecords() != null) {
                for (SQSEvent.SQSMessage msg : event.getRecords()) {
                    try {
                        JsonNode snsEnvelope = objectMapper.readTree(msg.getBody());
                        String innerMessage = snsEnvelope.get("Message").asText();
                        JsonNode payload = objectMapper.readTree(innerMessage);
                        String orderId = payload.get("orderId").asText();

                        orderService.handlePostPaymentSuccess(orderId, null);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed processing SQS message ID: " + msg.getMessageId(), e);
                    }
                }
            }
        } catch (Exception e) {
            if (subsegment != null) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            if (subsegment != null) {
                AWSXRay.endSubsegment();
            }
        }
        return null;
    }
}