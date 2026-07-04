package com.deva.paymentservice.service;

import com.deva.paymentservice.client.OrderClient;
import com.deva.paymentservice.dto.PaymentRequestDTO;
import com.deva.paymentservice.dto.PaymentResponseDTO;
import com.deva.paymentservice.entity.Payment;
import com.deva.paymentservice.exception.ResourceNotFoundException;
import com.deva.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
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
            case "SUCCESS"  -> "CONFIRMED";
            case "FAILED"   -> "CANCELLED";
            case "REFUNDED" -> "REFUNDED";
            default         -> null;
        };

        if (orderStatus != null) {
            orderClient.updateOrderStatus(payment.getOrderId(), orderStatus);
            if ("SUCCESS".equalsIgnoreCase(status)) {
                orderClient.handlePaymentSuccess(payment.getOrderId());
            }
        }

        return toResponse(updated);
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