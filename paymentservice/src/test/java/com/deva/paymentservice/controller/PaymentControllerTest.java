package com.deva.paymentservice.controller;

import com.deva.paymentservice.dto.PaymentRequestDTO;
import com.deva.paymentservice.dto.PaymentResponseDTO;
import com.deva.paymentservice.exception.ResourceNotFoundException;
import com.deva.paymentservice.security.UnauthorizedException;
import com.deva.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentRequestDTO requestDTO;
    private PaymentResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new PaymentRequestDTO();
        requestDTO.setOrderId("order-100");
        requestDTO.setAmount(99.99);
        requestDTO.setPaymentMethod("CREDIT_CARD");

        responseDTO = PaymentResponseDTO.builder()
                .paymentId("pay-1")
                .orderId("order-100")
                .userId("user-10")
                .amount(99.99)
                .paymentMethod("CREDIT_CARD")
                .status("PENDING")
                .createdAt("2026-08-01T10:00:00")
                .build();
    }

    @Test
    void createPayment_returnsCreated() {
        when(paymentService.createPayment(eq("user-10"), any(PaymentRequestDTO.class))).thenReturn(responseDTO);

        ResponseEntity<PaymentResponseDTO> response = paymentController.createPayment("user-10", requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPaymentId()).isEqualTo("pay-1");
        verify(paymentService).createPayment("user-10", requestDTO);
    }

    @Test
    void createPayment_serviceThrowsUnauthorized_propagates() {
        when(paymentService.createPayment(eq("user-10"), any(PaymentRequestDTO.class)))
                .thenThrow(new UnauthorizedException("Access denied"));

        assertThatThrownBy(() -> paymentController.createPayment("user-10", requestDTO))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getPaymentById_returnsOk() {
        when(paymentService.getPaymentById("pay-1")).thenReturn(responseDTO);

        ResponseEntity<PaymentResponseDTO> response = paymentController.getPaymentById("pay-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    void getPaymentById_notFound_propagates() {
        when(paymentService.getPaymentById("pay-1"))
                .thenThrow(new ResourceNotFoundException("Payment not found: pay-1"));

        assertThatThrownBy(() -> paymentController.getPaymentById("pay-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void getByOrderId_returnsOk() {
        when(paymentService.getPaymentsByOrderId("order-100")).thenReturn(List.of(responseDTO));

        ResponseEntity<List<PaymentResponseDTO>> response = paymentController.getByOrderId("order-100");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getByUserId_returnsOk() {
        when(paymentService.getPaymentsByUserId("user-10")).thenReturn(List.of(responseDTO));

        ResponseEntity<List<PaymentResponseDTO>> response = paymentController.getByUserId("user-10");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void updatePaymentStatus_returnsDto() {
        PaymentResponseDTO updated = PaymentResponseDTO.builder()
                .paymentId("pay-1")
                .orderId("order-100")
                .userId("user-10")
                .amount(99.99)
                .paymentMethod("CREDIT_CARD")
                .status("SUCCESS")
                .createdAt("2026-08-01T10:00:00")
                .build();
        when(paymentService.updatePaymentStatus("pay-1", "SUCCESS")).thenReturn(updated);

        PaymentResponseDTO result = paymentController.updatePaymentStatus("pay-1", "SUCCESS");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(paymentService).updatePaymentStatus("pay-1", "SUCCESS");
    }
}
