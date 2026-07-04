package com.deva.cartservice.controller;

import com.deva.cartservice.dto.CartRequestDTO;
import com.deva.cartservice.dto.CartResponseDTO;
import com.deva.cartservice.security.AuthUserId;
import com.deva.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartResponseDTO> addToCart(
            @AuthUserId String userId,
            @Valid @RequestBody CartRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addToCart(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<CartResponseDTO>> getCart(@AuthUserId String userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<CartResponseDTO> updateCartItem(
            @AuthUserId String userId,
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(userId, productId, quantity));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteCartItem(
            @AuthUserId String userId,
            @PathVariable String productId) {
        cartService.deleteCartItem(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthUserId String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}