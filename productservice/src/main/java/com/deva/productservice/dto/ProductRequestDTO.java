package com.deva.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class ProductRequestDTO {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "description is required")
    private String description;

    @NotBlank(message = "category is required")
    private String category;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private Double price;

    private String imageUrl;
    private List<String> imageUrls;
    private List<String> highlights;
}