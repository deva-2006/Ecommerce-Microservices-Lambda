package com.deva.paymentservice.service;

import com.deva.paymentservice.client.OrderClient;
import com.deva.paymentservice.dto.PaymentRequestDTO;
import com.deva.paymentservice.dto.PaymentResponseDTO;
import com.deva.paymentservice.entity.Payment;
import com.deva.paymentservice.exception.ResourceNotFoundException;
import com.deva.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import java.util.Map;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${sns.payment-events.topic.arn}")
    private String paymentEventsTopicArn;

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${cognito.userPoolId}")
    private String userPoolId;

    @Override
    public PaymentResponseDTO createPayment(String userId, PaymentRequestDTO request) {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID().toString())
                .orderId(request.getOrderId())
                .userId(userId)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status("PENDING")
                .createdAt(LocalDateTime.now().toString())
                .build();
        return toResponse(paymentRepository.save(payment));
    }

    @Override
    public PaymentResponseDTO getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        return toResponse(payment);
    }

    @Override
    public List<PaymentResponseDTO> getPaymentsByOrderId(String orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("No payments found for orderId: " + orderId);
        }
        return payments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponseDTO> getPaymentsByUserId(String userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("No payments found for userId: " + userId);
        }
        return payments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public PaymentResponseDTO updatePaymentStatus(String paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        payment.setStatus(status);
        Payment updated = paymentRepository.save(payment);

        String orderStatus = switch (status.toUpperCase()) {
            case "SUCCESS" -> "CONFIRMED";
            case "FAILED" -> "CANCELLED";
            case "REFUNDED" -> "REFUNDED";
            default -> null;
        };

        if (orderStatus != null) {
            orderClient.updateOrderStatus(payment.getOrderId(), orderStatus);
            if ("SUCCESS".equalsIgnoreCase(status)) {
                publishPaymentSuccessEvent(payment.getOrderId(), payment.getUserId());
            }
        }

        return toResponse(updated);
    }

    private void publishPaymentSuccessEvent(String orderId, String userId) {
        try {
            String email = fetchUserEmail(userId);
            Payment payment = paymentRepository.findByOrderId(orderId).stream().findFirst().orElse(null);

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("email", email);
            if (payment != null) {
                payload.put("amount", payment.getAmount());
                payload.put("paymentMethod", payment.getPaymentMethod());
                payload.put("paymentId", payment.getPaymentId());
                payload.put("timestamp", payment.getCreatedAt());
            }

            try {
                Map<String, Object> order = orderClient.getOrderById(orderId);
                if (order != null) {
                    payload.put("totalAmount", order.get("totalAmount"));
                    payload.put("shippingAddress", order.get("shippingAddress"));
                    payload.put("items", order.get("items"));
                }
            } catch (Exception e) {
                // Order fetch failed — proceed with partial data
            }

            String message = objectMapper.writeValueAsString(payload);

            snsClient.publish(
                    PublishRequest.builder()
                            .topicArn(paymentEventsTopicArn)
                            .message(message)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish payment success event", e);
        }
    }

    private String fetchUserEmail(String userId) {
        AdminGetUserResponse response = cognitoClient.adminGetUser(
                AdminGetUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(userId)
                        .build()
        );
        return response.userAttributes().stream()
                .filter(attr -> "email".equals(attr.name()))
                .map(AttributeType::value)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Email not found for user: " + userId));
    }
    private PaymentResponseDTO toResponse(Payment payment) {
        return PaymentResponseDTO.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}