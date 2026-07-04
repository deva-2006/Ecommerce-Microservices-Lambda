package com.deva.cartservice.service;

import com.deva.cartservice.client.InventoryClient;
import com.deva.cartservice.client.ProductClient;
import com.deva.cartservice.dto.CartRequestDTO;
import com.deva.cartservice.dto.CartResponseDTO;
import com.deva.cartservice.dto.ProductResponseDTO;
import com.deva.cartservice.entity.Cart;
import com.deva.cartservice.exception.ResourceNotFoundException;
import com.deva.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Override
    public CartResponseDTO addToCart(String userId, CartRequestDTO request) {

        // Step 1: Verify product exists + fetch latest name and price
        ProductResponseDTO product = productClient.getProductById(request.getProductId());

        // Step 2: Validate stock available in Inventory Service
        inventoryClient.validateStock(request.getProductId(), request.getQuantity());

        // Step 3: Build cart item using trusted data from Product Service
        Cart cart = Cart.builder()
                .userId(userId)
                .productId(product.getProductId())
                .productName(product.getName())
                .price(product.getPrice())
                .quantity(request.getQuantity())
                .totalPrice(product.getPrice() * request.getQuantity())
                .addedAt(LocalDateTime.now().toString())
                .build();

        return toResponse(cartRepository.save(cart));
    }

    @Override
    public List<CartResponseDTO> getCartByUserId(String userId) {
        List<Cart> items = cartRepository.findByUserId(userId);
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No cart items found for userId: " + userId);
        }
        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CartResponseDTO updateCartItem(String userId, String productId, Integer quantity) {
        Cart existing = cartRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found for userId: " + userId
                                + ", productId: " + productId));

        // Validate new quantity against Inventory Service
        inventoryClient.validateStock(productId, quantity);

        existing.setQuantity(quantity);
        existing.setTotalPrice(existing.getPrice() * quantity);

        return toResponse(cartRepository.save(existing));
    }

    @Override
    public void deleteCartItem(String userId, String productId) {
        cartRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found for userId: " + userId
                                + ", productId: " + productId));
        cartRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    public void clearCart(String userId) {
        List<Cart> items = cartRepository.findByUserId(userId);
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No cart found for userId: " + userId);
        }
        cartRepository.deleteAllByUserId(userId);
    }

    private CartResponseDTO toResponse(Cart cart) {
        return CartResponseDTO.builder()
                .userId(cart.getUserId())
                .productId(cart.getProductId())
                .productName(cart.getProductName())
                .price(cart.getPrice())
                .quantity(cart.getQuantity())
                .totalPrice(cart.getTotalPrice())
                .addedAt(cart.getAddedAt())
                .build();
    }
}