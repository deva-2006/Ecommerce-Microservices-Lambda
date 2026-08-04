package com.deva.orderservice.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderResponseDtoTest {

    @Test
    void builderAndGetters() {
        OrderResponseDTO dto = OrderResponseDTO.builder()
                .orderId("order-1")
                .paymentId("pay-1")
                .userId("user-1")
                .items(List.of())
                .totalAmount(1999.98)
                .status("PENDING")
                .shippingAddress("123 Main St")
                .createdAt("2026-08-01T10:00:00")
                .build();

        assertThat(dto.getOrderId()).isEqualTo("order-1");
        assertThat(dto.getPaymentId()).isEqualTo("pay-1");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getItems()).isEmpty();
        assertThat(dto.getTotalAmount()).isEqualTo(1999.98);
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getShippingAddress()).isEqualTo("123 Main St");
        assertThat(dto.getCreatedAt()).isEqualTo("2026-08-01T10:00:00");
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        OrderResponseDTO a = OrderResponseDTO.builder().orderId("order-1").build();
        OrderResponseDTO b = OrderResponseDTO.builder().orderId("order-1").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(OrderResponseDTO.builder().orderId("order-2").build());
    }
}
