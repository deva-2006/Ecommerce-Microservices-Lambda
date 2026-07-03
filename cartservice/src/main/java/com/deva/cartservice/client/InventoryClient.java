package com.deva.cartservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "inventory-service",
        url = "${gateway.url}"
)
public interface InventoryClient {

    @GetMapping("/inventory/{productId}/validate")
    void validateStock(@PathVariable String productId, @RequestParam Integer quantity);
}