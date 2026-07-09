package com.deva.orderservice.service;

import com.deva.orderservice.client.CartClient;
import com.deva.orderservice.client.InventoryClient;
import com.deva.orderservice.client.PaymentClient;
import com.deva.orderservice.client.ProductClient;
import com.deva.orderservice.dto.*;
import com.deva.orderservice.entity.Order;
import com.deva.orderservice.entity.OrderItem;
import com.deva.orderservice.exception.ResourceNotFoundException;
import com.deva.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    @Value("${internal.secret}")
    private String internalSecret;

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    @Override
    public OrderResponseDTO createOrder(String userId, OrderRequestDTO request) {

        List<CartItemDTO> cartItems = cartClient.getCartByUserId();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty for userId: " + userId);
        }

        for (CartItemDTO item : cartItems) {
            productClient.getProductById(item.getProductId());
            inventoryClient.validateStock(item.getProductId(), item.getQuantity());
        }

        List<OrderItem> orderItems = cartItems.stream()
                .map(item -> OrderItem.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        double totalAmount = orderItems.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();

        Order order = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .userId(userId)
                .items(orderItems)
                .totalAmount(totalAmount)
                .status("PENDING")
                .shippingAddress(request.getShippingAddress())
                .createdAt(LocalDateTime.now().toString())
                .build();

        orderRepository.save(order);

        PaymentResponseDTO payment = paymentClient.createPayment(
                PaymentRequestDTO.builder()
                        .orderId(order.getOrderId())
                        .amount(order.getTotalAmount())
                        .paymentMethod(request.getPaymentMethod())
                        .build()
        );

        order.setPaymentId(payment.getPaymentId());
        orderRepository.save(order);

        return toResponse(order);
    }
    @Override
    public void handlePostPaymentSuccess(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if ("FULFILLED".equals(order.getFulfillmentStatus())) {
            return; // already processed, skip — safe on retry
        }

        for (OrderItem item : order.getItems()) {
            inventoryClient.deductStock(item.getProductId(), new StockDeductRequestDTO(item.getQuantity()));
        }

        cartClient.clearCartInternal(order.getUserId(), internalSecret);

        order.setFulfillmentStatus("FULFILLED");
        orderRepository.save(order);
    }
    @Override
    public OrderResponseDTO getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + orderId));
        return toResponse(order);
    }

    @Override
    public List<OrderResponseDTO> getOrdersByUserId(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found for userId: " + userId);
        }
        return orders.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + orderId));
        order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    @Override
    public void deleteOrder(String orderId) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + orderId));
        orderRepository.deleteById(orderId);
    }

    private OrderResponseDTO toResponse(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> {
                    OrderItemDTO dto = new OrderItemDTO();
                    dto.setProductId(item.getProductId());
                    dto.setProductName(item.getProductName());
                    dto.setQuantity(item.getQuantity());
                    dto.setPrice(item.getPrice());
                    return dto;
                })
                .collect(Collectors.toList());

        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .paymentId(order.getPaymentId())
                .userId(order.getUserId())
                .items(itemDTOs)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }
}