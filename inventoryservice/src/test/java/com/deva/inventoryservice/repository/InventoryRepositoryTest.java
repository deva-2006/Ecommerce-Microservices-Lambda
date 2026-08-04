package com.deva.inventoryservice.repository;

import com.deva.inventoryservice.entity.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<Inventory> inventoryTable;

    private InventoryRepository repository;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("inventory-table"), any(TableSchema.class)))
                .thenReturn(inventoryTable);
        repository = new InventoryRepository(enhancedClient, "inventory-table");
    }

    @Test
    void constructor_buildsTableFromTableName() {
        verify(enhancedClient).table(eq("inventory-table"), any(TableSchema.class));
    }

    @Test
    void save_putsItemAndReturnsInventory() {
        Inventory inventory = Inventory.builder().productId("prod-1").quantity(10).build();

        Inventory result = repository.save(inventory);

        verify(inventoryTable).putItem(inventory);
        assertThat(result).isSameAs(inventory);
    }

    @Test
    void findByProductId_found_returnsInventory() {
        Inventory inventory = Inventory.builder().productId("prod-1").quantity(10).build();
        when(inventoryTable.getItem(any(Key.class))).thenReturn(inventory);

        Optional<Inventory> result = repository.findByProductId("prod-1");

        assertThat(result).containsSame(inventory);
        verify(inventoryTable).getItem(Key.builder().partitionValue("prod-1").build());
    }

    @Test
    void findByProductId_notFound_returnsEmpty() {
        when(inventoryTable.getItem(any(Key.class))).thenReturn(null);

        Optional<Inventory> result = repository.findByProductId("prod-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsItemsFromScan() {
        Inventory inventory = Inventory.builder().productId("prod-1").quantity(10).build();
        PageIterable<Inventory> pageIterable = mock(PageIterable.class);
        SdkIterable<Inventory> items = mock(SdkIterable.class);
        when(inventoryTable.scan(any(ScanEnhancedRequest.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(items);
        when(items.stream()).thenReturn(List.of(inventory).stream());

        List<Inventory> result = repository.findAll();

        assertThat(result).containsExactly(inventory);
        verify(inventoryTable).scan(any(ScanEnhancedRequest.class));
    }

    @Test
    void deleteByProductId_deletesByKey() {
        repository.deleteByProductId("prod-1");

        verify(inventoryTable).deleteItem(Key.builder().partitionValue("prod-1").build());
    }
}
