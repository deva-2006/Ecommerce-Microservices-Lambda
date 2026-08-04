package com.deva.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentResponseDtoTest {

    @Test
    void gettersAndSetters() {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setPaymentId("pay-1");
        dto.setOrderId("order-1");
        dto.setUserId("user-1");
        dto.setAmount(1999.98);
        dto.setPaymentMethod("CARD");
        dto.setStatus("SUCCESS");
        dto.setCreatedAt("2026-08-01T10:00:00");

        assertThat(dto.getPaymentId()).isEqualTo("pay-1");
        assertThat(dto.getOrderId()).isEqualTo("order-1");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getAmount()).isEqualTo(1999.98);
        assertThat(dto.getPaymentMethod()).isEqualTo("CARD");
        assertThat(dto.getStatus()).isEqualTo("SUCCESS");
        assertThat(dto.getCreatedAt()).isEqualTo("2026-08-01T10:00:00");
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        PaymentResponseDTO a = new PaymentResponseDTO();
        a.setPaymentId("pay-1");
        PaymentResponseDTO b = new PaymentResponseDTO();
        b.setPaymentId("pay-1");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(new PaymentResponseDTO());
    }
}
