package com.deva.productservice.controller;

import com.deva.productservice.dto.ProductRequestDTO;
import com.deva.productservice.dto.ProductResponseDTO;
import com.deva.productservice.dto.UploadUrlResponseDTO;
import com.deva.productservice.exception.ResourceNotFoundException;
import com.deva.productservice.service.ProductService;
import com.deva.productservice.service.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private S3Service s3Service;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductRequestDTO buildRequest() {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Laptop");
        request.setDescription("Gaming laptop");
        request.setCategory("Electronics");
        request.setPrice(1299.99);
        request.setImageUrls(List.of("https://img1.jpg"));
        request.setHighlights(List.of("Fast", "Lightweight"));
        return request;
    }

    private ProductResponseDTO buildResponse(String id) {
        return ProductResponseDTO.builder()
                .productId(id)
                .name("Laptop")
                .description("Gaming laptop")
                .category("Electronics")
                .price(1299.99)
                .imageUrl("https://img1.jpg")
                .imageUrls(List.of("https://img1.jpg"))
                .highlights(List.of("Fast", "Lightweight"))
                .build();
    }

    @Test
    void createProduct_shouldReturn201() throws Exception {
        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(buildResponse("new-id"));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("new-id"))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(1299.99));
    }

    @Test
    void createProduct_invalidBody_shouldReturn400() throws Exception {
        ProductRequestDTO invalidRequest = new ProductRequestDTO();
        invalidRequest.setName("");
        invalidRequest.setPrice(-5.0);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_shouldReturn200() throws Exception {
        List<ProductResponseDTO> products = List.of(buildResponse("1"), buildResponse("2"));
        when(productService.getAllProducts()).thenReturn(products);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productId").value("1"))
                .andExpect(jsonPath("$[1].productId").value("2"));
    }

    @Test
    void getAllProducts_emptyList_shouldReturn200() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getProductById_found_shouldReturn200() throws Exception {
        when(productService.getProductById("abc-123")).thenReturn(buildResponse("abc-123"));

        mockMvc.perform(get("/products/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("abc-123"))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void getProductById_notFound_shouldReturn404() throws Exception {
        when(productService.getProductById("missing"))
                .thenThrow(new ResourceNotFoundException("Product not found: missing"));

        mockMvc.perform(get("/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Product not found")));
    }

    @Test
    void getUploadUrl_shouldReturn200() throws Exception {
        UploadUrlResponseDTO uploadUrl = UploadUrlResponseDTO.builder()
                .uploadUrl("https://s3.example.com/upload")
                .publicUrl("https://s3.example.com/public/img.jpg")
                .build();
        when(s3Service.generatePresignedUploadUrl("photo.jpg", "image/jpeg")).thenReturn(uploadUrl);

        mockMvc.perform(get("/products/upload-url")
                        .param("fileName", "photo.jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("https://s3.example.com/upload"))
                .andExpect(jsonPath("$.publicUrl").value("https://s3.example.com/public/img.jpg"));
    }

    @Test
    void updateProduct_found_shouldReturn200() throws Exception {
        ProductResponseDTO updated = ProductResponseDTO.builder()
                .productId("abc-123")
                .name("Updated Laptop")
                .description("Updated Desc")
                .category("Computers")
                .price(1599.99)
                .imageUrl("https://new.jpg")
                .imageUrls(List.of("https://new.jpg"))
                .build();
        when(productService.updateProduct(eq("abc-123"), any(ProductRequestDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/products/abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.price").value(1599.99));
    }

    @Test
    void updateProduct_notFound_shouldReturn404() throws Exception {
        when(productService.updateProduct(eq("missing"), any(ProductRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Product not found: missing"));

        mockMvc.perform(put("/products/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_found_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/products/abc-123"))
                .andExpect(status().isNoContent());
        verify(productService).deleteProduct("abc-123");
    }

    @Test
    void deleteProduct_notFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found: missing"))
                .when(productService).deleteProduct("missing");

        mockMvc.perform(delete("/products/missing"))
                .andExpect(status().isNotFound());
    }
}
