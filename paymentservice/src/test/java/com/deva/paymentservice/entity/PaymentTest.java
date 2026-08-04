package com.deva.paymentservice.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @Test
    void builder_setsAllFields() {
        Payment payment = Payment.builder()
                .paymentId("pay-1")
                .orderId("order-1")
                .userId("user-1")
                .amount(99.99)
                .paymentMethod("CREDIT_CARD")
                .status("PENDING")
                .createdAt("2026-08-01T10:00:00")
                .build();

        assertThat(payment.getPaymentId()).isEqualTo("pay-1");
        assertThat(payment.getOrderId()).isEqualTo("order-1");
        assertThat(payment.getUserId()).isEqualTo("user-1");
        assertThat(payment.getAmount()).isEqualTo(99.99);
        assertThat(payment.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(payment.getCreatedAt()).isEqualTo("2026-08-01T10:00:00");
    }

    @Test
    void setters_updateValues() {
        Payment payment = new Payment();
        payment.setPaymentId("pay-2");
        payment.setOrderId("order-2");
        payment.setUserId("user-2");
        payment.setAmount(50.0);
        payment.setPaymentMethod("CASH");
        payment.setStatus("SUCCESS");
        payment.setCreatedAt("ts");

        assertThat(payment.getPaymentId()).isEqualTo("pay-2");
        assertThat(payment.getOrderId()).isEqualTo("order-2");
        assertThat(payment.getUserId()).isEqualTo("user-2");
        assertThat(payment.getAmount()).isEqualTo(50.0);
        assertThat(payment.getPaymentMethod()).isEqualTo("CASH");
        assertThat(payment.getStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getCreatedAt()).isEqualTo("ts");
    }

    @Test
    void allArgsConstructor_setsFields() {
        Payment payment = new Payment("pay-3", "order-3", "user-3", 25.0, "CASH", "PENDING", "ts");

        assertThat(payment.getPaymentId()).isEqualTo("pay-3");
        assertThat(payment.getOrderId()).isEqualTo("order-3");
        assertThat(payment.getUserId()).isEqualTo("user-3");
        assertThat(payment.getAmount()).isEqualTo(25.0);
        assertThat(payment.getPaymentMethod()).isEqualTo("CASH");
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(payment.getCreatedAt()).isEqualTo("ts");
    }
}
