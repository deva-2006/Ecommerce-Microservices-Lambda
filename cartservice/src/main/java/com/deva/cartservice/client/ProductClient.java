package com.deva.cartservice.client;

import com.deva.cartservice.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "product-service",
        url = "${gateway.url}"
)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponseDTO getProductById(@PathVariable String id);
}