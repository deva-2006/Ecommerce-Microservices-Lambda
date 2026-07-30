package com.deva.productservice.service;

import com.deva.productservice.dto.ProductRequestDTO;
import com.deva.productservice.dto.ProductResponseDTO;
import com.deva.productservice.entity.Product;
import com.deva.productservice.exception.ResourceNotFoundException;
import com.deva.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductRequestDTO buildRequest() {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setCategory("Electronics");
        request.setPrice(99.99);
        request.setImageUrls(List.of("https://img1.jpg", "https://img2.jpg"));
        request.setHighlights(List.of("Feature 1", "Feature 2"));
        return request;
    }

    private Product buildProduct(String id) {
        return Product.builder()
                .productId(id)
                .name("Test Product")
                .description("Test Description")
                .category("Electronics")
                .price(99.99)
                .imageUrl("https://img1.jpg")
                .imageUrls(List.of("https://img1.jpg", "https://img2.jpg"))
                .highlights(List.of("Feature 1", "Feature 2"))
                .build();
    }

    @Test
    void createProduct_shouldSaveAndReturnResponse() {
        ProductRequestDTO request = buildRequest();
        ProductResponseDTO response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getDescription()).isEqualTo("Test Description");
        assertThat(response.getCategory()).isEqualTo("Electronics");
        assertThat(response.getPrice()).isEqualTo(99.99);
        assertThat(response.getImageUrls()).containsExactly("https://img1.jpg", "https://img2.jpg");
        assertThat(response.getHighlights()).containsExactly("Feature 1", "Feature 2");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductId()).isNotNull();
        assertThat(saved.getImageUrl()).isEqualTo("https://img1.jpg");
    }

    @Test
    void createProduct_withSingleImageUrl_shouldResolveCorrectly() {
        ProductRequestDTO request = buildRequest();
        request.setImageUrls(null);
        request.setImageUrl("https://single.jpg");

        ProductResponseDTO response = productService.createProduct(request);

        assertThat(response.getImageUrls()).containsExactly("https://single.jpg");
        assertThat(response.getImageUrl()).isEqualTo("https://single.jpg");
    }

    @Test
    void createProduct_withNoImages_shouldReturnEmptyList() {
        ProductRequestDTO request = buildRequest();
        request.setImageUrls(null);
        request.setImageUrl(null);

        ProductResponseDTO response = productService.createProduct(request);

        assertThat(response.getImageUrls()).isEmpty();
        assertThat(response.getImageUrl()).isNull();
    }

    @Test
    void createProduct_imageUrlsTakesPriorityOverImageUrl() {
        ProductRequestDTO request = buildRequest();
        request.setImageUrl("https://single.jpg");
        request.setImageUrls(List.of("https://multi1.jpg", "https://multi2.jpg"));

        ProductResponseDTO response = productService.createProduct(request);

        assertThat(response.getImageUrls()).containsExactly("https://multi1.jpg", "https://multi2.jpg");
        assertThat(response.getImageUrl()).isEqualTo("https://multi1.jpg");
    }

    @Test
    void getProductById_found() {
        Product product = buildProduct("prod-123");
        when(productRepository.findById("prod-123")).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.getProductById("prod-123");

        assertThat(response.getProductId()).isEqualTo("prod-123");
        assertThat(response.getName()).isEqualTo("Test Product");
        verify(productRepository).findById("prod-123");
    }

    @Test
    void getProductById_notFound_shouldThrow() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found: missing");
    }

    @Test
    void getAllProducts_shouldReturnList() {
        List<Product> products = List.of(buildProduct("1"), buildProduct("2"), buildProduct("3"));
        when(productRepository.findAll()).thenReturn(products);

        List<ProductResponseDTO> result = productService.getAllProducts();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ProductResponseDTO::getProductId).containsExactly("1", "2", "3");
    }

    @Test
    void getAllProducts_emptyList() {
        when(productRepository.findAll()).thenReturn(new ArrayList<>());

        List<ProductResponseDTO> result = productService.getAllProducts();

        assertThat(result).isEmpty();
    }

    @Test
    void updateProduct_found() {
        Product existing = buildProduct("prod-1");
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));

        ProductRequestDTO request = buildRequest();
        request.setName("Updated Name");
        request.setPrice(149.99);
        request.setImageUrls(List.of("https://new.jpg"));

        ProductResponseDTO response = productService.updateProduct("prod-1", request);

        assertThat(response.getName()).isEqualTo("Updated Name");
        assertThat(response.getPrice()).isEqualTo(149.99);
        assertThat(response.getImageUrls()).containsExactly("https://new.jpg");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_notFound_shouldThrow() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        ProductRequestDTO request = buildRequest();

        assertThatThrownBy(() -> productService.updateProduct("missing", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found: missing");
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_found() {
        Product existing = buildProduct("prod-1");
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(existing));

        productService.deleteProduct("prod-1");

        verify(productRepository).delete("prod-1");
    }

    @Test
    void deleteProduct_notFound_shouldThrow() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found: missing");
        verify(productRepository, never()).delete(any());
    }

    @Test
    void createProduct_withEmptyImageUrls_shouldTreatAsEmpty() {
        ProductRequestDTO request = buildRequest();
        request.setImageUrls(List.of());
        request.setImageUrl(null);

        ProductResponseDTO response = productService.createProduct(request);

        assertThat(response.getImageUrls()).isEmpty();
        assertThat(response.getImageUrl()).isNull();
    }

    @Test
    void toResponse_withOnlyImageUrl_shouldBuildImageUrlsList() {
        Product product = Product.builder()
                .productId("p1")
                .name("Test")
                .description("Desc")
                .category("Cat")
                .price(10.0)
                .imageUrl("https://single.jpg")
                .imageUrls(null)
                .build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.getProductById("p1");

        assertThat(response.getImageUrls()).containsExactly("https://single.jpg");
    }

    @Test
    void toResponse_withNoImages_shouldReturnEmptyLists() {
        Product product = Product.builder()
                .productId("p1")
                .name("Test")
                .description("Desc")
                .category("Cat")
                .price(10.0)
                .imageUrl(null)
                .imageUrls(null)
                .build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.getProductById("p1");

        assertThat(response.getImageUrls()).isEmpty();
        assertThat(response.getImageUrl()).isNull();
    }
}
