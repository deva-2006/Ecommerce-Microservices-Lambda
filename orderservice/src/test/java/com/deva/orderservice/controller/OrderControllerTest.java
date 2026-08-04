package com.deva.orderservice.controller;

import com.deva.orderservice.dto.OrderRequestDTO;
import com.deva.orderservice.dto.OrderResponseDTO;
import com.deva.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private OrderRequestDTO requestDTO;
    private OrderResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new OrderRequestDTO();
        requestDTO.setShippingAddress("123 Main St");
        requestDTO.setPaymentMethod("CARD");

        responseDTO = OrderResponseDTO.builder()
                .orderId("order-1")
                .paymentId("pay-1")
                .userId("user-1")
                .items(List.of())
                .totalAmount(1999.98)
                .status("PENDING")
                .shippingAddress("123 Main St")
                .createdAt("2026-08-01T10:00:00")
                .build();
    }

    @Test
    void createOrder_returnsCreated() {
        when(orderService.createOrder("user-1", requestDTO)).thenReturn(responseDTO);

        ResponseEntity<OrderResponseDTO> response = orderController.createOrder("user-1", requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrderId()).isEqualTo("order-1");
        verify(orderService, times(1)).createOrder("user-1", requestDTO);
    }

    @Test
    void getOrderById_returnsOk() {
        when(orderService.getOrderById("order-1")).thenReturn(responseDTO);

        ResponseEntity<OrderResponseDTO> response = orderController.getOrderById("order-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo("user-1");
    }

    @Test
    void getOrdersByUserId_returnsOk() {
        when(orderService.getOrdersByUserId("user-1")).thenReturn(List.of(responseDTO));

        ResponseEntity<List<OrderResponseDTO>> response = orderController.getOrdersByUserId("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getAllOrders_returnsOk() {
        when(orderService.getAllOrders()).thenReturn(List.of(responseDTO));

        ResponseEntity<List<OrderResponseDTO>> response = orderController.getAllOrders();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void verifyPurchase_returnsPurchasedTrue() {
        when(orderService.hasUserPurchasedProduct("user-1", "prod-1")).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = orderController.verifyPurchase("user-1", "prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("purchased")).isTrue();
    }

    @Test
    void verifyPurchase_returnsPurchasedFalse() {
        when(orderService.hasUserPurchasedProduct("user-1", "prod-1")).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> response = orderController.verifyPurchase("user-1", "prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("purchased")).isFalse();
    }

    @Test
    void updateStatus_returnsOk() {
        when(orderService.updateOrderStatus("order-1", "SHIPPED")).thenReturn(responseDTO);

        ResponseEntity<OrderResponseDTO> response = orderController.updateStatus("order-1", "SHIPPED");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void deleteOrder_returnsNoContent() {
        doNothing().when(orderService).deleteOrder("order-1");

        ResponseEntity<Void> response = orderController.deleteOrder("order-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(orderService, times(1)).deleteOrder("order-1");
    }
}
