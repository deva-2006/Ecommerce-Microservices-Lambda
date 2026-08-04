package com.deva.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRequestDtoTest {

    @Test
    void gettersAndSetters() {
        OrderRequestDTO dto = new OrderRequestDTO();
        dto.setShippingAddress("123 Main St");
        dto.setPaymentMethod("CARD");

        assertThat(dto.getShippingAddress()).isEqualTo("123 Main St");
        assertThat(dto.getPaymentMethod()).isEqualTo("CARD");
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        OrderRequestDTO a = new OrderRequestDTO();
        a.setShippingAddress("123 Main St");
        OrderRequestDTO b = new OrderRequestDTO();
        b.setShippingAddress("123 Main St");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(new OrderRequestDTO());
    }
}
