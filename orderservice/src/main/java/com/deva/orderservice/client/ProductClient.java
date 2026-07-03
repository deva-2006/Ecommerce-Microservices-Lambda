package com.deva.orderservice.client;

import com.deva.orderservice.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "product-service",
        url = "${gateway.url}"
)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponseDTO getProductById(@PathVariable String id);

    @PutMapping("/products/{id}/deduct-stock")
    void deductStock(@PathVariable String id, @RequestParam int quantity);
}