package com.deva.productservice.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductRequestDTOTest {

    @Test
    void settersAndGetters_shouldWork() {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Laptop");
        dto.setDescription("Gaming laptop");
        dto.setCategory("Electronics");
        dto.setPrice(1299.99);
        dto.setImageUrl("https://img.jpg");
        dto.setImageUrls(List.of("https://img1.jpg", "https://img2.jpg"));
        dto.setHighlights(List.of("Fast", "Light"));

        assertThat(dto.getName()).isEqualTo("Laptop");
        assertThat(dto.getDescription()).isEqualTo("Gaming laptop");
        assertThat(dto.getCategory()).isEqualTo("Electronics");
        assertThat(dto.getPrice()).isEqualTo(1299.99);
        assertThat(dto.getImageUrl()).isEqualTo("https://img.jpg");
        assertThat(dto.getImageUrls()).containsExactly("https://img1.jpg", "https://img2.jpg");
        assertThat(dto.getHighlights()).containsExactly("Fast", "Light");
    }

    @Test
    void equals_sameValues_shouldBeEqual() {
        ProductRequestDTO a = new ProductRequestDTO();
        a.setName("Laptop");
        a.setDescription("Desc");
        a.setCategory("Cat");
        a.setPrice(100.0);

        ProductRequestDTO b = new ProductRequestDTO();
        b.setName("Laptop");
        b.setDescription("Desc");
        b.setCategory("Cat");
        b.setPrice(100.0);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentValues_shouldNotBeEqual() {
        ProductRequestDTO a = new ProductRequestDTO();
        a.setName("Laptop");

        ProductRequestDTO b = new ProductRequestDTO();
        b.setName("Phone");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_sameInstance_shouldBeEqual() {
        ProductRequestDTO dto = new ProductRequestDTO();
        assertThat(dto).isEqualTo(dto);
    }

    @Test
    void equals_null_shouldNotBeEqual() {
        ProductRequestDTO dto = new ProductRequestDTO();
        assertThat(dto).isNotEqualTo(null);
    }

    @Test
    void toString_shouldContainFieldNameAndValue() {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Test");

        String result = dto.toString();

        assertThat(result)
                .contains("name=Test")
                .contains("ProductRequestDTO");
    }

    @Test
    void equals_differentType_shouldNotBeEqual() {
        ProductRequestDTO dto = new ProductRequestDTO();
        assertThat(dto).isNotEqualTo("string");
    }

    @Test
    void setters_withNulls_shouldWork() {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName(null);
        dto.setDescription(null);
        dto.setCategory(null);
        dto.setPrice(null);

        assertThat(dto.getName()).isNull();
        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getCategory()).isNull();
        assertThat(dto.getPrice()).isNull();
    }
}
