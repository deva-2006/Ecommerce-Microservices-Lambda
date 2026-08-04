package com.deva.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemDtoTest {

    @Test
    void gettersAndSetters() {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setProductId("prod-1");
        dto.setProductName("Laptop");
        dto.setQuantity(2);
        dto.setPrice(999.99);

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getProductName()).isEqualTo("Laptop");
        assertThat(dto.getQuantity()).isEqualTo(2);
        assertThat(dto.getPrice()).isEqualTo(999.99);
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        OrderItemDTO a = new OrderItemDTO();
        a.setProductId("prod-1");
        OrderItemDTO b = new OrderItemDTO();
        b.setProductId("prod-1");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(new OrderItemDTO());
    }
}
