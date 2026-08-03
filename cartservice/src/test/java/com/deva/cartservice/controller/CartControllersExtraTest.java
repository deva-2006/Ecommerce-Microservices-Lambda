package com.deva.cartservice.controller;

import com.deva.cartservice.security.UnauthorizedException;
import com.deva.cartservice.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllersExtraTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartInternalController cartInternalController;

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController();
        ReflectionTestUtils.setField(cartInternalController, "internalSecret", "secret123");
    }

    @Test
    void clearCartInternal_validSecret_returnsNoContent() {
        doNothing().when(cartService).clearCart("user-55");

        ResponseEntity<Void> response = cartInternalController.clearCartInternal("user-55", "secret123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cartService, times(1)).clearCart("user-55");
    }

    @Test
    void clearCartInternal_invalidSecret_throwsUnauthorized() {
        assertThatThrownBy(() -> cartInternalController.clearCartInternal("user-55", "wrong-secret"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid internal secret");
    }

    @Test
    void health_returnsOkWithStatusUp() {
        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("Cart Service");
    }
}
