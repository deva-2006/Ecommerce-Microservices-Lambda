package com.deva.orderservice.service;

import com.deva.orderservice.dto.OrderRequestDTO;
import com.deva.orderservice.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {
    OrderResponseDTO createOrder(String userId, OrderRequestDTO request);
    OrderResponseDTO getOrderById(String orderId);
    List<OrderResponseDTO> getOrdersByUserId(String userId);
    List<OrderResponseDTO> getAllOrders();
    OrderResponseDTO updateOrderStatus(String orderId, String status);
    void deleteOrder(String orderId);
    void handlePostPaymentSuccess(String orderId, String userId);
    boolean hasUserPurchasedProduct(String userId, String productId);
}