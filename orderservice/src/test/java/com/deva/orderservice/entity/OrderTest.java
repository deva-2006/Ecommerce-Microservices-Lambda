package com.deva.orderservice.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void builderAndGetters() {
        OrderItem item = OrderItem.builder()
                .productId("prod-1")
                .productName("Laptop")
                .quantity(2)
                .price(999.99)
                .subtotal(1999.98)
                .build();

        Order order = Order.builder()
                .orderId("order-1")
                .paymentId("pay-1")
                .userId("user-1")
                .items(List.of(item))
                .totalAmount(1999.98)
                .status("PENDING")
                .shippingAddress("123 Main St")
                .createdAt("2026-08-01T10:00:00")
                .fulfillmentStatus("PENDING")
                .build();

        assertThat(order.getOrderId()).isEqualTo("order-1");
        assertThat(order.getPaymentId()).isEqualTo("pay-1");
        assertThat(order.getUserId()).isEqualTo("user-1");
        assertThat(order.getItems()).containsExactly(item);
        assertThat(order.getTotalAmount()).isEqualTo(1999.98);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getShippingAddress()).isEqualTo("123 Main St");
        assertThat(order.getCreatedAt()).isEqualTo("2026-08-01T10:00:00");
        assertThat(order.getFulfillmentStatus()).isEqualTo("PENDING");
    }

    @Test
    void setters() {
        Order order = new Order();
        order.setOrderId("order-2");
        order.setStatus("SHIPPED");

        assertThat(order.getOrderId()).isEqualTo("order-2");
        assertThat(order.getStatus()).isEqualTo("SHIPPED");
    }
}
