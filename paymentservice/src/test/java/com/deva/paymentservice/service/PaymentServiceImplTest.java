package com.deva.paymentservice.service;

import com.deva.paymentservice.client.OrderClient;
import com.deva.paymentservice.dto.PaymentRequestDTO;
import com.deva.paymentservice.dto.PaymentResponseDTO;
import com.deva.paymentservice.entity.Payment;
import com.deva.paymentservice.exception.ResourceNotFoundException;
import com.deva.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private SnsClient snsClient;

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentServiceImpl paymentService;

    private PaymentRequestDTO requestDTO;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, orderClient, snsClient, objectMapper, cognitoClient);
        ReflectionTestUtils.setField(paymentService, "paymentEventsTopicArn",
                "arn:aws:sns:us-east-1:123456789012:payment-events");
        ReflectionTestUtils.setField(paymentService, "userPoolId", "us-east-1_pool");

        requestDTO = new PaymentRequestDTO();
        requestDTO.setOrderId("order-100");
        requestDTO.setAmount(99.99);
        requestDTO.setPaymentMethod("CREDIT_CARD");

        payment = Payment.builder()
                .paymentId("pay-1")
                .orderId("order-100")
                .userId("user-10")
                .amount(99.99)
                .paymentMethod("CREDIT_CARD")
                .status("PENDING")
                .createdAt("2026-08-01T10:00:00")
                .build();
    }

    @Test
    void createPayment_success() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponseDTO response = paymentService.createPayment("user-10", requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order-100");
        assertThat(response.getUserId()).isEqualTo("user-10");
        assertThat(response.getAmount()).isEqualTo(99.99);
        assertThat(response.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getPaymentId()).isNotBlank();
        assertThat(response.getCreatedAt()).isNotBlank();
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void getPaymentById_success() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));

        PaymentResponseDTO response = paymentService.getPaymentById("pay-1");

        assertThat(response.getPaymentId()).isEqualTo("pay-1");
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getPaymentById_notFound_throwsResourceNotFound() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById("pay-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found: pay-1");
    }

    @Test
    void getPaymentsByOrderId_success() {
        when(paymentRepository.findByOrderId("order-100")).thenReturn(List.of(payment));

        List<PaymentResponseDTO> result = paymentService.getPaymentsByOrderId("order-100");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo("order-100");
    }

    @Test
    void getPaymentsByOrderId_empty_throwsResourceNotFound() {
        when(paymentRepository.findByOrderId("order-100")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> paymentService.getPaymentsByOrderId("order-100"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No payments found for orderId: order-100");
    }

    @Test
    void getPaymentsByUserId_success() {
        when(paymentRepository.findByUserId("user-10")).thenReturn(List.of(payment));

        List<PaymentResponseDTO> result = paymentService.getPaymentsByUserId("user-10");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-10");
    }

    @Test
    void getPaymentsByUserId_empty_throwsResourceNotFound() {
        when(paymentRepository.findByUserId("user-10")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> paymentService.getPaymentsByUserId("user-10"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No payments found for userId: user-10");
    }

    @Test
    void updatePaymentStatus_success_updatesOrderAndPublishesEvent() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentRepository.findByOrderId("order-100")).thenReturn(List.of(payment));
        AdminGetUserResponse adminResponse = mockAdminGetUserResponse("user@example.com");
        when(cognitoClient.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(adminResponse);
        when(orderClient.getOrderById("order-100")).thenReturn(Map.of(
                "totalAmount", 99.99,
                "shippingAddress", "123 Main St",
                "items", List.of("item-1")));
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().build());

        PaymentResponseDTO response = paymentService.updatePaymentStatus("pay-1", "SUCCESS");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(paymentRepository, times(1)).save(payment);
        verify(orderClient, times(1)).updateOrderStatus("order-100", "CONFIRMED");

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient, times(1)).publish(captor.capture());
        PublishRequest publish = captor.getValue();
        assertThat(publish.topicArn()).isEqualTo("arn:aws:sns:us-east-1:123456789012:payment-events");
        assertThat(publish.message()).contains("user@example.com").contains("order-100");
    }

    @Test
    void updatePaymentStatus_failed_cancelsOrderWithoutPublishing() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponseDTO response = paymentService.updatePaymentStatus("pay-1", "FAILED");

        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(orderClient, times(1)).updateOrderStatus("order-100", "CANCELLED");
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void updatePaymentStatus_refunded_updatesOrderWithoutPublishing() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponseDTO response = paymentService.updatePaymentStatus("pay-1", "REFUNDED");

        assertThat(response.getStatus()).isEqualTo("REFUNDED");
        verify(orderClient, times(1)).updateOrderStatus("order-100", "REFUNDED");
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void updatePaymentStatus_unknownStatus_skipsOrderUpdate() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponseDTO response = paymentService.updatePaymentStatus("pay-1", "PROCESSING");

        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        verify(orderClient, never()).updateOrderStatus(anyString(), anyString());
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void updatePaymentStatus_lowercaseSuccess_stillPublishes() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentRepository.findByOrderId("order-100")).thenReturn(List.of(payment));
        AdminGetUserResponse adminResponse = mockAdminGetUserResponse("user@example.com");
        when(cognitoClient.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(adminResponse);
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().build());

        PaymentResponseDTO response = paymentService.updatePaymentStatus("pay-1", "success");

        assertThat(response.getStatus()).isEqualTo("success");
        verify(orderClient, times(1)).updateOrderStatus("order-100", "CONFIRMED");
        verify(snsClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    void updatePaymentStatus_notFound_throwsResourceNotFound() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updatePaymentStatus("pay-1", "SUCCESS"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found: pay-1");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void updatePaymentStatus_publishFailure_wrapsInRuntimeException() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentRepository.findByOrderId("order-100")).thenReturn(List.of(payment));
        AdminGetUserResponse adminResponse = mockAdminGetUserResponse("user@example.com");
        when(cognitoClient.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(adminResponse);
        when(snsClient.publish(any(PublishRequest.class))).thenThrow(new RuntimeException("SNS down"));

        assertThatThrownBy(() -> paymentService.updatePaymentStatus("pay-1", "SUCCESS"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to publish payment success event");
        verify(orderClient, times(1)).updateOrderStatus("order-100", "CONFIRMED");
    }

    @Test
    void updatePaymentStatus_emailMissing_wrapsInRuntimeException() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        AdminGetUserResponse adminResponse = mockAdminGetUserResponseWithAttributes(List.of(
                AttributeType.builder().name("username").value("user-10").build()));
        when(cognitoClient.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(adminResponse);

        assertThatThrownBy(() -> paymentService.updatePaymentStatus("pay-1", "SUCCESS"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to publish payment success event");
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void updatePaymentStatus_orderFetchFails_publishesWithPartialData() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentRepository.findByOrderId("order-100")).thenReturn(List.of(payment));
        AdminGetUserResponse adminResponse = mockAdminGetUserResponse("user@example.com");
        when(cognitoClient.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(adminResponse);
        when(orderClient.getOrderById("order-100")).thenThrow(new RuntimeException("Order service down"));
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().build());

        PaymentResponseDTO response = paymentService.updatePaymentStatus("pay-1", "SUCCESS");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(snsClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    void updatePaymentStatus_paymentNotFoundForEvent_publishesWithoutPaymentFields() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentRepository.findByOrderId("order-100")).thenReturn(Collections.emptyList());
        AdminGetUserResponse adminResponse = mockAdminGetUserResponse("user@example.com");
        when(cognitoClient.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(adminResponse);
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().build());

        paymentService.updatePaymentStatus("pay-1", "SUCCESS");

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient, times(1)).publish(captor.capture());
        assertThat(captor.getValue().message()).contains("order-100").doesNotContain("amount");
    }

    private AdminGetUserResponse mockAdminGetUserResponse(String email) {
        return mockAdminGetUserResponseWithAttributes(List.of(
                AttributeType.builder().name("email").value(email).build()));
    }

    private AdminGetUserResponse mockAdminGetUserResponseWithAttributes(List<AttributeType> attributes) {
        AdminGetUserResponse response = mock(AdminGetUserResponse.class);
        when(response.userAttributes()).thenReturn(attributes);
        return response;
    }
}
