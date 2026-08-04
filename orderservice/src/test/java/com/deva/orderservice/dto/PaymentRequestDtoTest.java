package com.deva.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestDtoTest {

    @Test
    void builderAndGetters() {
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .orderId("order-1")
                .amount(1999.98)
                .paymentMethod("CARD")
                .build();

        assertThat(dto.getOrderId()).isEqualTo("order-1");
        assertThat(dto.getAmount()).isEqualTo(1999.98);
        assertThat(dto.getPaymentMethod()).isEqualTo("CARD");
    }

    @Test
    void constructors() {
        PaymentRequestDTO allArgs = new PaymentRequestDTO("order-1", 100.0, "CASH");
        PaymentRequestDTO noArgs = new PaymentRequestDTO();

        assertThat(allArgs.getOrderId()).isEqualTo("order-1");
        assertThat(allArgs.getAmount()).isEqualTo(100.0);
        assertThat(allArgs.getPaymentMethod()).isEqualTo("CASH");
        assertThat(noArgs.getOrderId()).isNull();
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        PaymentRequestDTO a = PaymentRequestDTO.builder().orderId("order-1").amount(100.0).build();
        PaymentRequestDTO b = new PaymentRequestDTO("order-1", 100.0, null);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(PaymentRequestDTO.builder().orderId("order-2").build());
    }
}
