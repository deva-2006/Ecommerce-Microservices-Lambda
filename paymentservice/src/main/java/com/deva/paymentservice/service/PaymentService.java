package com.deva.paymentservice.service;

import com.deva.paymentservice.dto.PaymentRequestDTO;
import com.deva.paymentservice.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {
    PaymentResponseDTO createPayment(String userId, PaymentRequestDTO request);
    PaymentResponseDTO getPaymentById(String paymentId);
    List<PaymentResponseDTO> getPaymentsByOrderId(String orderId);
    List<PaymentResponseDTO> getPaymentsByUserId(String userId);
    PaymentResponseDTO updatePaymentStatus(String paymentId, String status);
}