package com.deva.paymentservice.repository;

import com.deva.paymentservice.entity.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<Payment> table;

    @Mock
    private PageIterable<Payment> pageIterable;

    @Mock
    private SdkIterable<Payment> items;

    private PaymentRepository repository;

    private Payment payment;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(anyString(), ArgumentMatchers.<TableSchema<Payment>>any())).thenReturn(table);
        repository = new PaymentRepository(enhancedClient, "Payments");

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
    void save_persistsAndReturnsPayment() {
        Payment result = repository.save(payment);

        assertThat(result).isSameAs(payment);
        verify(table).putItem(payment);
    }

    @Test
    void findById_found_returnsOptionalOfPayment() {
        when(table.getItem(any(Key.class))).thenReturn(payment);

        Optional<Payment> result = repository.findById("pay-1");

        assertThat(result).contains(payment);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        Optional<Payment> result = repository.findById("pay-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsAllItems() {
        when(table.scan(any(ScanEnhancedRequest.class))).thenReturn(pageIterable);
        stubScanItems(List.of(payment, otherPayment("pay-2", "order-200", "user-20")));

        List<Payment> result = repository.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void findByOrderId_filtersByOrder() {
        when(table.scan(any(ScanEnhancedRequest.class))).thenReturn(pageIterable);
        stubScanItems(List.of(
                payment,
                otherPayment("pay-2", "order-200", "user-20"),
                otherPayment("pay-3", "order-100", "user-30")));

        List<Payment> result = repository.findByOrderId("order-100");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Payment::getPaymentId).containsExactly("pay-1", "pay-3");
    }

    @Test
    void findByUserId_filtersByUser() {
        when(table.scan(any(ScanEnhancedRequest.class))).thenReturn(pageIterable);
        stubScanItems(List.of(
                payment,
                otherPayment("pay-2", "order-200", "user-20"),
                otherPayment("pay-3", "order-300", "user-10")));

        List<Payment> result = repository.findByUserId("user-10");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Payment::getPaymentId).containsExactly("pay-1", "pay-3");
    }

    private void stubScanItems(List<Payment> payments) {
        when(pageIterable.items()).thenReturn(items);
        when(items.stream()).thenReturn(payments.stream());
    }

    @Test
    void deleteById_deletesItem() {
        repository.deleteById("pay-1");

        verify(table).deleteItem(any(Key.class));
    }

    private Payment otherPayment(String id, String orderId, String userId) {
        return Payment.builder()
                .paymentId(id)
                .orderId(orderId)
                .userId(userId)
                .amount(50.0)
                .paymentMethod("CASH")
                .status("PENDING")
                .createdAt("ts")
                .build();
    }
}
