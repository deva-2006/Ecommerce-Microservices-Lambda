package com.deva.orderservice.client;

import com.deva.orderservice.dto.CartItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "cart-service",
        url = "${gateway.url}"
)
public interface CartClient {

    @GetMapping("/cart/{userId}")
    List<CartItemDTO> getCartByUserId(@PathVariable String userId);

    @DeleteMapping("/cart/{userId}")
    void clearCart(@PathVariable String userId);
}