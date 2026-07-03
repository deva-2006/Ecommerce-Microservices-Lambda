package com.deva.productservice.service;

import com.deva.productservice.dto.ProductRequestDTO;
import com.deva.productservice.dto.ProductResponseDTO;
import com.deva.productservice.entity.Product;
import com.deva.productservice.exception.ResourceNotFoundException;
import com.deva.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Product product = Product.builder()
                .productId(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .build();
        productRepository.save(product);           // save first
        return toResponse(product);                // then return as response object
    }

    @Override
    public ProductResponseDTO getProductById(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));
        return toResponse(product);
    }

    @Override
    public List<ProductResponseDTO> getAllProducts() {

        System.out.println("===== SERVICE START =====");

        List<Product> products = productRepository.findAll();

        System.out.println("===== FINDALL SUCCESS =====");

        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    @Override
    public ProductResponseDTO updateProduct(String productId, ProductRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        productRepository.save(product);           // save first
        return toResponse(product);                // then return
    }

    @Override
    public void deleteProduct(String productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));
        productRepository.delete(productId);       // delete() not deleteById()
    }

    private ProductResponseDTO toResponse(Product product) {
        return ProductResponseDTO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .build();
    }
}