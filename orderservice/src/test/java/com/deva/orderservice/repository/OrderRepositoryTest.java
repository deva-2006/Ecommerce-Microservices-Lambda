package com.deva.orderservice.repository;

import com.deva.orderservice.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<Order> orderTable;

    private OrderRepository repository;

    private Order order;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(orderTable);
        repository = new OrderRepository(enhancedClient, "Orders");
        order = Order.builder().orderId("order-1").userId("user-1").status("PENDING").build();
    }

    @Test
    void save_persistsAndReturnsOrder() {
        Order result = repository.save(order);

        assertThat(result).isSameAs(order);
        verify(orderTable).putItem(order);
    }

    @Test
    void findById_returnsOrderWhenPresent() {
        when(orderTable.getItem(any(Key.class))).thenReturn(order);

        Optional<Order> result = repository.findById("order-1");

        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo("order-1");
    }

    @Test
    void findById_returnsEmptyWhenMissing() {
        when(orderTable.getItem(any(Key.class))).thenReturn(null);

        assertThat(repository.findById("order-1")).isEmpty();
    }

    @Test
    void findByUserId_filtersByUserId() {
        Order other = Order.builder().orderId("order-2").userId("user-2").build();
        PageIterable<Order> page = mock(PageIterable.class);
        SdkIterable<Order> iterable = mock(SdkIterable.class);
        when(orderTable.scan(any(ScanEnhancedRequest.class))).thenReturn(page);
        when(page.items()).thenReturn(iterable);
        when(iterable.stream()).thenReturn(Stream.of(order, other));

        List<Order> result = repository.findByUserId("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-1");
    }

    @Test
    void findAll_returnsAllOrders() {
        Order other = Order.builder().orderId("order-2").userId("user-2").build();
        PageIterable<Order> page = mock(PageIterable.class);
        SdkIterable<Order> iterable = mock(SdkIterable.class);
        when(orderTable.scan(any(ScanEnhancedRequest.class))).thenReturn(page);
        when(page.items()).thenReturn(iterable);
        when(iterable.stream()).thenReturn(Stream.of(order, other));

        List<Order> result = repository.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void deleteById_deletesOrder() {
        repository.deleteById("order-1");

        verify(orderTable).deleteItem(any(Key.class));
    }
}
