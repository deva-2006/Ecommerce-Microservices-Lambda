package com.deva.paymentservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentResponseDTOTest {

    @Test
    void builder_setsAllFields() {
        PaymentResponseDTO dto = PaymentResponseDTO.builder()
                .paymentId("pay-1")
                .orderId("order-1")
                .userId("user-1")
                .amount(99.99)
                .paymentMethod("CREDIT_CARD")
                .status("SUCCESS")
                .createdAt("2026-08-01T10:00:00")
                .build();

        assertThat(dto.getPaymentId()).isEqualTo("pay-1");
        assertThat(dto.getOrderId()).isEqualTo("order-1");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getAmount()).isEqualTo(99.99);
        assertThat(dto.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(dto.getStatus()).isEqualTo("SUCCESS");
        assertThat(dto.getCreatedAt()).isEqualTo("2026-08-01T10:00:00");
    }

    @Test
    void setters_updateValues() {
        PaymentResponseDTO dto = PaymentResponseDTO.builder().build();
        dto.setStatus("FAILED");
        dto.setAmount(10.0);

        assertThat(dto.getStatus()).isEqualTo("FAILED");
        assertThat(dto.getAmount()).isEqualTo(10.0);
    }
}
