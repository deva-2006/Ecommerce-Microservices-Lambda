package com.deva.productservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductResponseDTO {
    private String productId;
    private String name;
    private String description;
    private String category;
    private Double price;
    private String imageUrl;
    private List<String> imageUrls;
    private List<String> highlights;
}