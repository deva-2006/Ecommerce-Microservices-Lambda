package com.deva.productservice.repository;

import com.deva.productservice.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryTest {

    @Mock
    private DynamoDbTable<Product> table;

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    private ProductRepository repository;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(enhancedClient.table(any(), any())).thenReturn((DynamoDbTable) table);
        repository = new ProductRepository(enhancedClient, "test-table");
    }

    @Test
    void save_shouldCallPutItem() {
        Product product = Product.builder()
                .productId("prod-1")
                .name("Test")
                .price(99.99)
                .build();

        repository.save(product);

        verify(table).putItem(product);
    }

    @Test
    void findById_shouldReturnProduct_whenExists() {
        Product product = Product.builder()
                .productId("prod-1")
                .name("Test")
                .price(99.99)
                .build();
        when(table.getItem(any(Key.class))).thenReturn(product);

        Optional<Product> result = repository.findById("prod-1");

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("prod-1");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        Optional<Product> result = repository.findById("missing");

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldReturnList() {
        Product p1 = Product.builder().productId("1").name("P1").price(10.0).build();
        Product p2 = Product.builder().productId("2").name("P2").price(20.0).build();

        PageIterable<Product> mockPage = mock(PageIterable.class);
        SdkIterable<Product> mockItems = mock(SdkIterable.class);
        when(table.scan()).thenReturn(mockPage);
        when(mockPage.items()).thenReturn(mockItems);
        when(mockItems.stream()).thenReturn(java.util.stream.Stream.of(p1, p2));

        List<Product> result = repository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getProductId).containsExactly("1", "2");
    }

    @Test
    void delete_shouldCallDeleteItem() {
        repository.delete("prod-1");

        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);
        verify(table).deleteItem(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).isNotNull();
    }
}
