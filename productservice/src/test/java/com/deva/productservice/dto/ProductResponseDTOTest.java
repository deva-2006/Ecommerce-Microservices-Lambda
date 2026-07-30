package com.deva.productservice.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResponseDTOTest {

    @Test
    void builder_shouldBuildCorrectly() {
        ProductResponseDTO dto = ProductResponseDTO.builder()
                .productId("prod-1")
                .name("Laptop")
                .description("Gaming laptop")
                .category("Electronics")
                .price(1299.99)
                .imageUrl("https://img.jpg")
                .imageUrls(List.of("https://img1.jpg", "https://img2.jpg"))
                .highlights(List.of("Fast", "Light"))
                .build();

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getName()).isEqualTo("Laptop");
        assertThat(dto.getDescription()).isEqualTo("Gaming laptop");
        assertThat(dto.getCategory()).isEqualTo("Electronics");
        assertThat(dto.getPrice()).isEqualTo(1299.99);
        assertThat(dto.getImageUrl()).isEqualTo("https://img.jpg");
        assertThat(dto.getImageUrls()).containsExactly("https://img1.jpg", "https://img2.jpg");
        assertThat(dto.getHighlights()).containsExactly("Fast", "Light");
    }

    @Test
    void settersAndGetters_shouldWork() {
        ProductResponseDTO dto = ProductResponseDTO.builder().build();
        dto.setProductId("prod-1");
        dto.setName("Phone");
        dto.setDescription("Smartphone");
        dto.setCategory("Mobile");
        dto.setPrice(999.99);
        dto.setImageUrl("https://phone.jpg");
        dto.setImageUrls(List.of("https://p1.jpg"));
        dto.setHighlights(List.of("5G"));

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getName()).isEqualTo("Phone");
        assertThat(dto.getDescription()).isEqualTo("Smartphone");
        assertThat(dto.getCategory()).isEqualTo("Mobile");
        assertThat(dto.getPrice()).isEqualTo(999.99);
        assertThat(dto.getImageUrl()).isEqualTo("https://phone.jpg");
        assertThat(dto.getImageUrls()).containsExactly("https://p1.jpg");
        assertThat(dto.getHighlights()).containsExactly("5G");
    }

    @Test
    void equals_sameValues_shouldBeEqual() {
        ProductResponseDTO a = ProductResponseDTO.builder()
                .productId("1").name("Test").price(10.0).build();
        ProductResponseDTO b = ProductResponseDTO.builder()
                .productId("1").name("Test").price(10.0).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentValues_shouldNotBeEqual() {
        ProductResponseDTO a = ProductResponseDTO.builder()
                .productId("1").name("A").build();
        ProductResponseDTO b = ProductResponseDTO.builder()
                .productId("2").name("B").build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_sameInstance_shouldBeEqual() {
        ProductResponseDTO dto = ProductResponseDTO.builder().name("Test").build();
        assertThat(dto).isEqualTo(dto);
    }

    @Test
    void equals_null_shouldNotBeEqual() {
        ProductResponseDTO dto = ProductResponseDTO.builder().build();
        assertThat(dto).isNotEqualTo(null);
    }

    @Test
    void equals_differentType_shouldNotBeEqual() {
        ProductResponseDTO dto = ProductResponseDTO.builder().build();
        assertThat(dto).isNotEqualTo("string");
    }

    @Test
    void toString_shouldContainFieldInfo() {
        ProductResponseDTO dto = ProductResponseDTO.builder()
                .productId("1").name("Laptop").build();

        String result = dto.toString();

        assertThat(result).contains("productId=1");
        assertThat(result).contains("name=Laptop");
        assertThat(result).contains("ProductResponseDTO");
    }

    @Test
    void builder_withNulls_shouldBuild() {
        ProductResponseDTO dto = ProductResponseDTO.builder().build();

        assertThat(dto.getProductId()).isNull();
        assertThat(dto.getName()).isNull();
        assertThat(dto.getPrice()).isNull();
        assertThat(dto.getImageUrls()).isNull();
    }
}
