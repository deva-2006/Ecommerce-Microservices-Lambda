package com.deva.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemDtoTest {

    @Test
    void gettersAndSetters() {
        CartItemDTO dto = new CartItemDTO();
        dto.setUserId("user-1");
        dto.setProductId("prod-1");
        dto.setProductName("Laptop");
        dto.setPrice(999.99);
        dto.setQuantity(2);
        dto.setTotalPrice(1999.98);
        dto.setAddedAt("2026-08-01T10:00:00");

        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getProductName()).isEqualTo("Laptop");
        assertThat(dto.getPrice()).isEqualTo(999.99);
        assertThat(dto.getQuantity()).isEqualTo(2);
        assertThat(dto.getTotalPrice()).isEqualTo(1999.98);
        assertThat(dto.getAddedAt()).isEqualTo("2026-08-01T10:00:00");
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        CartItemDTO a = new CartItemDTO();
        a.setProductId("prod-1");
        CartItemDTO b = new CartItemDTO();
        b.setProductId("prod-1");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(new CartItemDTO());
    }
}
