package com.deva.cartservice.service;

import com.deva.cartservice.dto.CartRequestDTO;
import com.deva.cartservice.dto.CartResponseDTO;

import java.util.List;

public interface CartService {
    CartResponseDTO addToCart(String userId, CartRequestDTO request);
    List<CartResponseDTO> getCartByUserId(String userId);
    CartResponseDTO updateCartItem(String userId, String productId, Integer quantity);
    void deleteCartItem(String userId, String productId);
    void clearCart(String userId);
}