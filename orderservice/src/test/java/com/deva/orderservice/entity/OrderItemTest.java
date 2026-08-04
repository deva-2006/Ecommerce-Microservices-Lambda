package com.deva.orderservice.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    @Test
    void builderAndGetters() {
        OrderItem item = OrderItem.builder()
                .productId("prod-1")
                .productName("Laptop")
                .quantity(2)
                .price(999.99)
                .subtotal(1999.98)
                .build();

        assertThat(item.getProductId()).isEqualTo("prod-1");
        assertThat(item.getProductName()).isEqualTo("Laptop");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getPrice()).isEqualTo(999.99);
        assertThat(item.getSubtotal()).isEqualTo(1999.98);
    }

    @Test
    void setters() {
        OrderItem item = new OrderItem();
        item.setQuantity(5);

        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        OrderItem a = OrderItem.builder().productId("prod-1").build();
        OrderItem b = OrderItem.builder().productId("prod-1").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(OrderItem.builder().productId("prod-2").build());
    }
}
