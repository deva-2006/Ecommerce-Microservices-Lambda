package com.deva.cartservice.controller;

import com.deva.cartservice.dto.CartRequestDTO;
import com.deva.cartservice.dto.CartResponseDTO;
import com.deva.cartservice.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private CartRequestDTO requestDTO;
    private CartResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new CartRequestDTO();
        requestDTO.setProductId("prod-100");
        requestDTO.setQuantity(3);

        responseDTO = CartResponseDTO.builder()
                .userId("user-10")
                .productId("prod-100")
                .productName("Keyboard")
                .price(49.99)
                .quantity(3)
                .totalPrice(149.97)
                .build();
    }

    @Test
    void addToCart_returnsCreated() {
        when(cartService.addToCart(eq("user-10"), any(CartRequestDTO.class))).thenReturn(responseDTO);

        ResponseEntity<CartResponseDTO> response = cartController.addToCart("user-10", requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProductName()).isEqualTo("Keyboard");
    }

    @Test
    void getCart_returnsOk() {
        when(cartService.getCartByUserId("user-10")).thenReturn(List.of(responseDTO));

        ResponseEntity<List<CartResponseDTO>> response = cartController.getCart("user-10");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void updateCartItem_returnsOk() {
        when(cartService.updateCartItem("user-10", "prod-100", 5)).thenReturn(responseDTO);

        ResponseEntity<CartResponseDTO> response = cartController.updateCartItem("user-10", "prod-100", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void deleteCartItem_returnsNoContent() {
        doNothing().when(cartService).deleteCartItem("user-10", "prod-100");

        ResponseEntity<Void> response = cartController.deleteCartItem("user-10", "prod-100");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cartService, times(1)).deleteCartItem("user-10", "prod-100");
    }

    @Test
    void clearCart_returnsNoContent() {
        doNothing().when(cartService).clearCart("user-10");

        ResponseEntity<Void> response = cartController.clearCart("user-10");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cartService, times(1)).clearCart("user-10");
    }
}
