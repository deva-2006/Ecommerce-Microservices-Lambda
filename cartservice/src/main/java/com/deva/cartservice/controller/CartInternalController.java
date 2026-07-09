package com.deva.cartservice.controller;

import com.deva.cartservice.security.UnauthorizedException;
import com.deva.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CartInternalController {

    private final CartService cartService;

    @Value("${internal.secret}")
    private String internalSecret;

    @DeleteMapping("/internal/cart/{userId}")
    public ResponseEntity<Void> clearCartInternal(
            @PathVariable String userId,
            @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) {
            throw new UnauthorizedException("Invalid internal secret");
        }
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}