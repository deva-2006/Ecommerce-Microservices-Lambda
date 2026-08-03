package com.deva.cartservice.service;

import com.deva.cartservice.client.InventoryClient;
import com.deva.cartservice.client.ProductClient;
import com.deva.cartservice.dto.CartRequestDTO;
import com.deva.cartservice.dto.CartResponseDTO;
import com.deva.cartservice.dto.ProductResponseDTO;
import com.deva.cartservice.entity.Cart;
import com.deva.cartservice.exception.ResourceNotFoundException;
import com.deva.cartservice.repository.CartRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private CartRequestDTO requestDTO;
    private ProductResponseDTO productDTO;
    private Cart cart;

    @BeforeEach
    void setUp() {
        requestDTO = new CartRequestDTO();
        requestDTO.setProductId("prod-123");
        requestDTO.setQuantity(2);

        productDTO = new ProductResponseDTO();
        productDTO.setProductId("prod-123");
        productDTO.setName("Laptop");
        productDTO.setPrice(999.99);


        cart = Cart.builder()
                .userId("user-1")
                .productId("prod-123")
                .productName("Laptop")
                .price(999.99)
                .quantity(2)
                .totalPrice(1999.98)
                .addedAt("2026-08-01T10:00:00")
                .build();
    }

    @Test
    void addToCart_success() {
        when(productClient.getProductById("prod-123")).thenReturn(productDTO);
        doNothing().when(inventoryClient).validateStock("prod-123", 2);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDTO response = cartService.addToCart("user-1", requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getProductName()).isEqualTo("Laptop");
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void addToCart_productClientException_throwsResourceNotFound() {
        when(productClient.getProductById("prod-123")).thenThrow(new RuntimeException("Product service down"));

        assertThatThrownBy(() -> cartService.addToCart("user-1", requestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist or has been deleted");
    }

    @Test
    void addToCart_productNull_throwsResourceNotFound() {
        when(productClient.getProductById("prod-123")).thenReturn(null);

        assertThatThrownBy(() -> cartService.addToCart("user-1", requestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void addToCart_inventoryNotFound_throwsResourceNotFound() {
        when(productClient.getProductById("prod-123")).thenReturn(productDTO);
        FeignException feignException = mockFeignException(404);
        doThrow(feignException).when(inventoryClient).validateStock("prod-123", 2);

        assertThatThrownBy(() -> cartService.addToCart("user-1", requestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventory not found");
    }

    @Test
    void addToCart_inventoryInsufficientStock_throwsIllegalArgument() {
        when(productClient.getProductById("prod-123")).thenReturn(productDTO);
        FeignException feignException = mockFeignException(400);
        doThrow(feignException).when(inventoryClient).validateStock("prod-123", 2);

        assertThatThrownBy(() -> cartService.addToCart("user-1", requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock available");
    }

    @Test
    void addToCart_inventoryGeneralError_throwsIllegalArgument() {
        when(productClient.getProductById("prod-123")).thenReturn(productDTO);
        FeignException feignException = mockFeignException(500);
        doThrow(feignException).when(inventoryClient).validateStock("prod-123", 2);

        assertThatThrownBy(() -> cartService.addToCart("user-1", requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock validation failed");
    }

    @Test
    void getCartByUserId_success() {
        when(cartRepository.findByUserId("user-1")).thenReturn(List.of(cart));

        List<CartResponseDTO> result = cartService.getCartByUserId("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo("prod-123");
    }

    @Test
    void getCartByUserId_empty_throwsResourceNotFound() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> cartService.getCartByUserId("user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No cart items found");
    }

    @Test
    void updateCartItem_success() {
        when(cartRepository.findByUserIdAndProductId("user-1", "prod-123")).thenReturn(Optional.of(cart));
        doNothing().when(inventoryClient).validateStock("prod-123", 5);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDTO response = cartService.updateCartItem("user-1", "prod-123", 5);

        assertThat(response).isNotNull();
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void updateCartItem_notFound_throwsResourceNotFound() {
        when(cartRepository.findByUserIdAndProductId("user-1", "prod-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateCartItem("user-1", "prod-123", 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCartItem_success() {
        when(cartRepository.findByUserIdAndProductId("user-1", "prod-123")).thenReturn(Optional.of(cart));
        doNothing().when(cartRepository).deleteByUserIdAndProductId("user-1", "prod-123");

        cartService.deleteCartItem("user-1", "prod-123");

        verify(cartRepository, times(1)).deleteByUserIdAndProductId("user-1", "prod-123");
    }

    @Test
    void deleteCartItem_notFound_throwsResourceNotFound() {
        when(cartRepository.findByUserIdAndProductId("user-1", "prod-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.deleteCartItem("user-1", "prod-123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clearCart_success() {
        when(cartRepository.findByUserId("user-1")).thenReturn(List.of(cart));
        doNothing().when(cartRepository).deleteAllByUserId("user-1");

        cartService.clearCart("user-1");

        verify(cartRepository, times(1)).deleteAllByUserId("user-1");
    }

    @Test
    void clearCart_notFound_throwsResourceNotFound() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> cartService.clearCart("user-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private FeignException mockFeignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/url", Map.of(), null, null, null);
        return FeignException.errorStatus("methodKey", Response.builder()
                .status(status)
                .reason("Error")
                .request(request)
                .build());
    }
}
