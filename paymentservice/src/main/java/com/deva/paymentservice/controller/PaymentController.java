package com.deva.paymentservice.controller;

import com.deva.paymentservice.dto.PaymentRequestDTO;
import com.deva.paymentservice.dto.PaymentResponseDTO;
import com.deva.paymentservice.security.AuthUserId;
import com.deva.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @AuthUserId String userId,
            @Valid @RequestBody PaymentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponseDTO>> getByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(orderId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<PaymentResponseDTO>> getByUserId(@AuthUserId String userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }

    @PutMapping("/{paymentId}/status")
    public PaymentResponseDTO updatePaymentStatus(
            @PathVariable String paymentId,
            @RequestParam String status) {

        return paymentService.updatePaymentStatus(paymentId, status);
    }
}