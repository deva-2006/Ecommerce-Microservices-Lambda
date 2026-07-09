package com.deva.orderservice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.deva.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;

public class PaymentSuccessSqsHandler implements RequestHandler<SQSEvent, Void> {

    private static final ConfigurableApplicationContext context =
            SpringApplication.run(OrderServiceApplication.class); // your @SpringBootApplication class

    private final OrderService orderService = context.getBean(OrderService.class);
    private final ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

    @Override
    public Void handleRequest(SQSEvent event, Context lambdaContext) {
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                JsonNode snsEnvelope = objectMapper.readTree(msg.getBody());
                String innerMessage = snsEnvelope.get("Message").asText();
                JsonNode payload = objectMapper.readTree(innerMessage);
                String orderId = payload.get("orderId").asText();

                orderService.handlePostPaymentSuccess(orderId, null);
            } catch (Exception e) {
                throw new RuntimeException("Failed processing SQS message: " + msg.getMessageId(), e);
            }
        }
        return null;
    }
}