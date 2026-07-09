package com.deva.orderservice.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import com.deva.orderservice.dto.CartItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "cart-service",
        url = "${gateway.url}"
)
public interface CartClient {

    @DeleteMapping("/internal/cart/{userId}")
    void clearCartInternal(@PathVariable String userId, @RequestHeader("X-Internal-Secret") String secret);

    @GetMapping("/cart")
    List<CartItemDTO> getCartByUserId();

    @DeleteMapping("/cart")
    void clearCart();
}