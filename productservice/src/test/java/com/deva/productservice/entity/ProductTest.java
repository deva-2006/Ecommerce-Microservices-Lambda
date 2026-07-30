package com.deva.productservice.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void builder_shouldBuildCorrectly() {
        Product product = Product.builder()
                .productId("prod-1")
                .name("Laptop")
                .description("Gaming laptop")
                .category("Electronics")
                .price(1299.99)
                .imageUrl("https://img.jpg")
                .imageUrls(List.of("https://img1.jpg", "https://img2.jpg"))
                .highlights(List.of("Fast", "Light"))
                .build();

        assertThat(product.getProductId()).isEqualTo("prod-1");
        assertThat(product.getName()).isEqualTo("Laptop");
        assertThat(product.getDescription()).isEqualTo("Gaming laptop");
        assertThat(product.getCategory()).isEqualTo("Electronics");
        assertThat(product.getPrice()).isEqualTo(1299.99);
        assertThat(product.getImageUrl()).isEqualTo("https://img.jpg");
        assertThat(product.getImageUrls()).containsExactly("https://img1.jpg", "https://img2.jpg");
        assertThat(product.getHighlights()).containsExactly("Fast", "Light");
    }

    @Test
    void noArgsConstructor_shouldCreateEmptyProduct() {
        Product product = new Product();

        assertThat(product.getProductId()).isNull();
        assertThat(product.getName()).isNull();
        assertThat(product.getPrice()).isNull();
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        Product product = new Product(
                "prod-1", "Laptop", "Desc", "Cat", 100.0,
                "https://img.jpg", List.of("https://img.jpg"), List.of("Fast")
        );

        assertThat(product.getProductId()).isEqualTo("prod-1");
        assertThat(product.getName()).isEqualTo("Laptop");
        assertThat(product.getDescription()).isEqualTo("Desc");
        assertThat(product.getCategory()).isEqualTo("Cat");
        assertThat(product.getPrice()).isEqualTo(100.0);
        assertThat(product.getImageUrl()).isEqualTo("https://img.jpg");
        assertThat(product.getImageUrls()).containsExactly("https://img.jpg");
        assertThat(product.getHighlights()).containsExactly("Fast");
    }

    @Test
    void settersAndGetters_shouldWork() {
        Product product = new Product();
        product.setProductId("1");
        product.setName("Phone");
        product.setDescription("Smartphone");
        product.setCategory("Mobile");
        product.setPrice(999.99);
        product.setImageUrl("https://phone.jpg");
        product.setImageUrls(List.of("https://p1.jpg"));
        product.setHighlights(List.of("5G"));

        assertThat(product.getProductId()).isEqualTo("1");
        assertThat(product.getName()).isEqualTo("Phone");
        assertThat(product.getDescription()).isEqualTo("Smartphone");
        assertThat(product.getCategory()).isEqualTo("Mobile");
        assertThat(product.getPrice()).isEqualTo(999.99);
        assertThat(product.getImageUrl()).isEqualTo("https://phone.jpg");
        assertThat(product.getImageUrls()).containsExactly("https://p1.jpg");
        assertThat(product.getHighlights()).containsExactly("5G");
    }

    @Test
    void getProductId_shouldReturnPartitionKey() {
        Product product = Product.builder()
                .productId("pk-123")
                .build();

        assertThat(product.getProductId()).isEqualTo("pk-123");
    }

    @Test
    void builder_withNulls_shouldBuild() {
        Product product = Product.builder().build();

        assertThat(product.getProductId()).isNull();
        assertThat(product.getName()).isNull();
        assertThat(product.getPrice()).isNull();
        assertThat(product.getImageUrls()).isNull();
        assertThat(product.getHighlights()).isNull();
    }

    @Test
    void setters_withNulls_shouldWork() {
        Product product = new Product();
        product.setProductId(null);
        product.setName(null);
        product.setPrice(null);
        product.setImageUrl(null);
        product.setImageUrls(null);
        product.setHighlights(null);

        assertThat(product.getProductId()).isNull();
        assertThat(product.getName()).isNull();
        assertThat(product.getPrice()).isNull();
        assertThat(product.getImageUrl()).isNull();
        assertThat(product.getImageUrls()).isNull();
        assertThat(product.getHighlights()).isNull();
    }
}
