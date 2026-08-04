package com.deva.productservice.service;

import com.deva.productservice.dto.ProductRequestDTO;
import com.deva.productservice.dto.ProductResponseDTO;
import com.deva.productservice.entity.Product;
import com.deva.productservice.exception.ResourceNotFoundException;
import com.deva.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private List<String> resolveImageUrls(ProductRequestDTO request) {
        List<String> imageUrls = request.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls;
        }
        String single = request.getImageUrl();
        if (single != null && !single.isBlank()) {
            return List.of(single);
        }
        return List.of();
    }

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        List<String> imageUrls = resolveImageUrls(request);
        Product product = Product.builder()
                .productId(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .highlights(request.getHighlights())
                .imageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
                .imageUrls(imageUrls)
                .build();
        productRepository.save(product);
        return toResponse(product);
    }

    private static final String PRODUCT_NOT_FOUND_MSG = "Product not found: ";

    @Override
    public ProductResponseDTO getProductById(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        PRODUCT_NOT_FOUND_MSG + productId));
        return toResponse(product);
    }

    @Override
    public List<ProductResponseDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProductResponseDTO updateProduct(String productId, ProductRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        PRODUCT_NOT_FOUND_MSG + productId));
        List<String> imageUrls = resolveImageUrls(request);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setHighlights(request.getHighlights());
        product.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        product.setImageUrls(imageUrls);
        productRepository.save(product);
        return toResponse(product);
    }

    @Override
    public void deleteProduct(String productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        PRODUCT_NOT_FOUND_MSG + productId));
        productRepository.delete(productId);
    }

    private List<String> buildResponseImageUrls(Product product) {
        List<String> imageUrls = product.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls;
        }
        String single = product.getImageUrl();
        if (single != null && !single.isBlank()) {
            return List.of(single);
        }
        return new ArrayList<>();
    }

    private ProductResponseDTO toResponse(Product product) {
        List<String> imageUrls = buildResponseImageUrls(product);
        String imageUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);
        return ProductResponseDTO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .highlights(product.getHighlights())
                .imageUrl(imageUrl)
                .imageUrls(imageUrls)
                .build();
    }
}