package com.deva.orderservice.client;

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

    @GetMapping("/cart")
    List<CartItemDTO> getCartByUserId();

    @DeleteMapping("/cart")
    void clearCart();
}