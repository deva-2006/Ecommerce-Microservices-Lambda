package com.deva.cartservice.repository;

import com.deva.cartservice.entity.Cart;
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
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<Cart> cartTable;

    private CartRepository cartRepository;

    private Cart cart;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("cart-table"), any(TableSchema.class))).thenReturn(cartTable);
        cartRepository = new CartRepository(enhancedClient, "cart-table");

        cart = Cart.builder()
                .userId("user-1")
                .productId("prod-1")
                .productName("Keyboard")
                .price(49.99)
                .quantity(2)
                .totalPrice(99.98)
                .addedAt("2026-08-01T10:00:00")
                .build();
    }

    @Test
    void constructor_buildsTableForTableName() {
        verify(enhancedClient).table(eq("cart-table"), any(TableSchema.class));
    }

    @Test
    void save_putsItemAndReturnsIt() {
        Cart saved = cartRepository.save(cart);

        assertThat(saved).isSameAs(cart);
        verify(cartTable).putItem(cart);
    }

    @Test
    void findByUserId_returnsAllMatchingItems() {
        Cart second = Cart.builder().userId("user-1").productId("prod-2").productName("Mouse").build();
        PageIterable<Cart> pageIterable = mock(PageIterable.class);
        SdkIterable<Cart> sdkIterable = mock(SdkIterable.class);
        when(cartTable.query(any(QueryConditional.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(sdkIterable);
        when(sdkIterable.stream()).thenReturn(Stream.of(cart, second));

        List<Cart> items = cartRepository.findByUserId("user-1");

        assertThat(items).containsExactly(cart, second);
    }

    @Test
    void findByUserIdAndProductId_returnsCartWhenPresent() {
        when(cartTable.getItem(any(Key.class))).thenReturn(cart);

        Optional<Cart> result = cartRepository.findByUserIdAndProductId("user-1", "prod-1");

        assertThat(result).contains(cart);
    }

    @Test
    void findByUserIdAndProductId_returnsEmptyWhenAbsent() {
        when(cartTable.getItem(any(Key.class))).thenReturn(null);

        Optional<Cart> result = cartRepository.findByUserIdAndProductId("user-1", "prod-1");

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByUserIdAndProductId_deletesItem() {
        cartRepository.deleteByUserIdAndProductId("user-1", "prod-1");

        verify(cartTable).deleteItem(any(Key.class));
    }

    @Test
    void deleteAllByUserId_deletesEachFoundItem() {
        Cart second = Cart.builder().userId("user-1").productId("prod-2").productName("Mouse").build();
        PageIterable<Cart> pageIterable = mock(PageIterable.class);
        SdkIterable<Cart> sdkIterable = mock(SdkIterable.class);
        when(cartTable.query(any(QueryConditional.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(sdkIterable);
        when(sdkIterable.stream()).thenReturn(Stream.of(cart, second));

        cartRepository.deleteAllByUserId("user-1");

        verify(cartTable, times(2)).deleteItem(any(Key.class));
    }
}
