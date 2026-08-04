package com.deva.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResponseDtoTest {

    @Test
    void gettersAndSetters() {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setProductId("prod-1");
        dto.setName("Laptop");
        dto.setDescription("A powerful laptop");
        dto.setPrice(999.99);
        dto.setCategory("Electronics");
        dto.setStock(10);

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getName()).isEqualTo("Laptop");
        assertThat(dto.getDescription()).isEqualTo("A powerful laptop");
        assertThat(dto.getPrice()).isEqualTo(999.99);
        assertThat(dto.getCategory()).isEqualTo("Electronics");
        assertThat(dto.getStock()).isEqualTo(10);
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        ProductResponseDTO a = new ProductResponseDTO();
        a.setProductId("prod-1");
        ProductResponseDTO b = new ProductResponseDTO();
        b.setProductId("prod-1");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(new ProductResponseDTO());
    }
}
