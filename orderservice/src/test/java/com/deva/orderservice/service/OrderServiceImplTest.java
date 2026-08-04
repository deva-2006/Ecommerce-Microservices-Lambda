package com.deva.orderservice.service;

import com.deva.orderservice.client.CartClient;
import com.deva.orderservice.client.InventoryClient;
import com.deva.orderservice.client.PaymentClient;
import com.deva.orderservice.client.ProductClient;
import com.deva.orderservice.dto.CartItemDTO;
import com.deva.orderservice.dto.OrderRequestDTO;
import com.deva.orderservice.dto.OrderResponseDTO;
import com.deva.orderservice.dto.PaymentRequestDTO;
import com.deva.orderservice.dto.PaymentResponseDTO;
import com.deva.orderservice.dto.ProductResponseDTO;
import com.deva.orderservice.dto.StockDeductRequestDTO;
import com.deva.orderservice.entity.Order;
import com.deva.orderservice.entity.OrderItem;
import com.deva.orderservice.exception.ResourceNotFoundException;
import com.deva.orderservice.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartClient cartClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CartItemDTO cartItem;
    private OrderItem orderItem;
    private Order order;
    private OrderRequestDTO request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "internalSecret", "secret");

        request = new OrderRequestDTO();
        request.setShippingAddress("123 Main St");
        request.setPaymentMethod("CARD");

        cartItem = new CartItemDTO();
        cartItem.setUserId("user-1");
        cartItem.setProductId("prod-1");
        cartItem.setProductName("Laptop");
        cartItem.setPrice(1000.0);
        cartItem.setQuantity(2);
        cartItem.setTotalPrice(2000.0);

        orderItem = OrderItem.builder()
                .productId("prod-1")
                .productName("Laptop")
                .quantity(2)
                .price(1000.0)
                .subtotal(2000.0)
                .build();

        order = Order.builder()
                .orderId("order-1")
                .paymentId("pay-1")
                .userId("user-1")
                .items(List.of(orderItem))
                .totalAmount(2000.0)
                .status("PENDING")
                .shippingAddress("123 Main St")
                .createdAt("2026-08-01T10:00:00")
                .fulfillmentStatus("PENDING")
                .build();
    }

    @Test
    void createOrder_success() {
        when(cartClient.getCartByUserId()).thenReturn(List.of(cartItem));
        when(productClient.getProductById("prod-1")).thenReturn(new ProductResponseDTO());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        PaymentResponseDTO payment = new PaymentResponseDTO();
        payment.setPaymentId("pay-1");
        when(paymentClient.createPayment(any(PaymentRequestDTO.class))).thenReturn(payment);

        OrderResponseDTO response = orderService.createOrder("user-1", request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo("pay-1");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Laptop");
        assertThat(response.getTotalAmount()).isEqualTo(2000.0);
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(paymentClient).createPayment(any(PaymentRequestDTO.class));
    }

    @Test
    void createOrder_emptyCart_throwsIllegalState() {
        when(cartClient.getCartByUserId()).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder("user-1", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    void createOrder_nullCart_throwsIllegalState() {
        when(cartClient.getCartByUserId()).thenReturn(null);

        assertThatThrownBy(() -> orderService.createOrder("user-1", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    void createOrder_productUnavailable_throwsIllegalArgument() {
        when(cartClient.getCartByUserId()).thenReturn(List.of(cartItem));
        when(productClient.getProductById("prod-1")).thenThrow(mockFeignException(404));

        assertThatThrownBy(() -> orderService.createOrder("user-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Laptop' is no longer available");
    }

    @Test
    void createOrder_productUnavailable_nullName_usesDefaultName() {
        cartItem.setProductName(null);
        when(cartClient.getCartByUserId()).thenReturn(List.of(cartItem));
        when(productClient.getProductById("prod-1")).thenThrow(mockFeignException(404));

        assertThatThrownBy(() -> orderService.createOrder("user-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Product' is no longer available");
    }

    @Test
    void createOrder_outOfStock_throwsIllegalArgument() {
        when(cartClient.getCartByUserId()).thenReturn(List.of(cartItem));
        when(productClient.getProductById("prod-1")).thenReturn(new ProductResponseDTO());
        doThrow(mockFeignException(400)).when(inventoryClient).validateStock("prod-1", 2);

        assertThatThrownBy(() -> orderService.createOrder("user-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Laptop' is currently out of stock");
    }

    @Test
    void createOrder_outOfStock_nullName_usesDefaultName() {
        cartItem.setProductName(null);
        when(cartClient.getCartByUserId()).thenReturn(List.of(cartItem));
        when(productClient.getProductById("prod-1")).thenReturn(new ProductResponseDTO());
        doThrow(mockFeignException(400)).when(inventoryClient).validateStock("prod-1", 2);

        assertThatThrownBy(() -> orderService.createOrder("user-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Product' is currently out of stock");
    }

    @Test
    void handlePostPaymentSuccess_success() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        orderService.handlePostPaymentSuccess("order-1", "user-1");

        assertThat(order.getFulfillmentStatus()).isEqualTo("FULFILLED");
        verify(inventoryClient).deductStock("prod-1", new StockDeductRequestDTO(2));
        verify(cartClient).clearCartInternal("user-1", "secret");
        verify(orderRepository).save(order);
    }

    @Test
    void handlePostPaymentSuccess_notFound_throwsResourceNotFound() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.handlePostPaymentSuccess("order-1", "user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void handlePostPaymentSuccess_alreadyFulfilled_returnsEarly() {
        order.setFulfillmentStatus("FULFILLED");
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        orderService.handlePostPaymentSuccess("order-1", "user-1");

        verifyNoInteractions(inventoryClient, cartClient);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void getOrderById_success() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        OrderResponseDTO response = orderService.getOrderById("order-1");

        assertThat(response.getOrderId()).isEqualTo("order-1");
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void getOrderById_notFound_throwsResourceNotFound() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("order-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found: order-1");
    }

    @Test
    void getOrdersByUserId_success() {
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of(order));

        List<OrderResponseDTO> result = orderService.getOrdersByUserId("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-1");
    }

    @Test
    void getOrdersByUserId_empty_returnsEmptyList() {
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of());

        assertThat(orderService.getOrdersByUserId("user-1")).isEmpty();
    }

    @Test
    void getOrdersByUserId_null_returnsEmptyList() {
        when(orderRepository.findByUserId("user-1")).thenReturn(null);

        assertThat(orderService.getOrdersByUserId("user-1")).isEmpty();
    }

    @Test
    void getAllOrders_success() {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderResponseDTO> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllOrders_empty_returnsEmptyList() {
        when(orderRepository.findAll()).thenReturn(List.of());

        assertThat(orderService.getAllOrders()).isEmpty();
    }

    @Test
    void getAllOrders_null_returnsEmptyList() {
        when(orderRepository.findAll()).thenReturn(null);

        assertThat(orderService.getAllOrders()).isEmpty();
    }

    @Test
    void updateOrderStatus_success() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDTO response = orderService.updateOrderStatus("order-1", "SHIPPED");

        assertThat(response.getStatus()).isEqualTo("SHIPPED");
        assertThat(order.getStatus()).isEqualTo("SHIPPED");
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_notFound_throwsResourceNotFound() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus("order-1", "SHIPPED"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteOrder_success() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        orderService.deleteOrder("order-1");

        verify(orderRepository).deleteById("order-1");
    }

    @Test
    void deleteOrder_notFound_throwsResourceNotFound() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder("order-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void hasUserPurchasedProduct_true() {
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of(order));

        assertThat(orderService.hasUserPurchasedProduct("user-1", "prod-1")).isTrue();
    }

    @Test
    void hasUserPurchasedProduct_falseForUnknownProduct() {
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of(order));

        assertThat(orderService.hasUserPurchasedProduct("user-1", "prod-999")).isFalse();
    }

    @Test
    void hasUserPurchasedProduct_emptyOrders_returnsFalse() {
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of());

        assertThat(orderService.hasUserPurchasedProduct("user-1", "prod-1")).isFalse();
    }

    @Test
    void hasUserPurchasedProduct_nullOrders_returnsFalse() {
        when(orderRepository.findByUserId("user-1")).thenReturn(null);

        assertThat(orderService.hasUserPurchasedProduct("user-1", "prod-1")).isFalse();
    }

    @Test
    void hasUserPurchasedProduct_cancelledOrdersAreIgnored() {
        order.setStatus("CANCELLED");
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of(order));

        assertThat(orderService.hasUserPurchasedProduct("user-1", "prod-1")).isFalse();
    }

    @Test
    void hasUserPurchasedProduct_nullItemsAreIgnored() {
        order.setItems(null);
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of(order));

        assertThat(orderService.hasUserPurchasedProduct("user-1", "prod-1")).isFalse();
    }

    @Test
    void hasUserPurchasedProduct_skipsCancelledThenMatchesActive() {
        Order cancelled = Order.builder().orderId("order-c").userId("user-1").status("CANCELLED")
                .items(List.of(OrderItem.builder().productId("prod-1").build())).build();
        Order active = Order.builder().orderId("order-a").userId("user-1").status("COMPLETED")
                .items(List.of(OrderItem.builder().productId("prod-1").build())).build();
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of(cancelled, active));

        assertThat(orderService.hasUserPurchasedProduct("user-1", "prod-1")).isTrue();
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
