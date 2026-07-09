package com.deva.orderservice.controller;

import com.deva.orderservice.dto.OrderRequestDTO;
import com.deva.orderservice.dto.OrderResponseDTO;
import com.deva.orderservice.security.AuthUserId;
import com.deva.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @AuthUserId String userId,
            @Valid @RequestBody OrderRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUserId(@AuthUserId String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    /*
     * Old synchronous flow (Feign):
     * Payment Service -> Order Service -> handlePostPaymentSuccess()
     *
     * Replaced by event-driven architecture:
     * Payment Service -> SNS -> SQS -> PaymentSuccessSqsHandler -> handlePostPaymentSuccess()
     */

// @PostMapping("/{id}/payment-success")
// public ResponseEntity<Void> handlePaymentSuccess(
//         @PathVariable String id,
//         @AuthUserId String userId) {
//     orderService.handlePostPaymentSuccess(id, userId);
//     return ResponseEntity.noContent().build();
// }//    @PostMapping("/{id}/payment-success")
//    public ResponseEntity<Void> handlePaymentSuccess(
//            @PathVariable String id,
//            @AuthUserId String userId) {
//        orderService.handlePostPaymentSuccess(id, userId);
//        return ResponseEntity.noContent().build();
//    }
}