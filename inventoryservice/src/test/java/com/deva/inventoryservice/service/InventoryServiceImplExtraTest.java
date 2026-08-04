package com.deva.inventoryservice.service;

import com.deva.inventoryservice.entity.Inventory;
import com.deva.inventoryservice.exception.ResourceNotFoundException;
import com.deva.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplExtraTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .productId("prod-99")
                .quantity(50)
                .updatedAt("2026-08-01T12:00:00")
                .build();
    }

    @Test
    void addStock_notFound_throwsResourceNotFound() {
        when(inventoryRepository.findByProductId("prod-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.addStock("prod-99", 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("prod-99");
        verify(inventoryRepository, never()).save(inventory);
    }

    @Test
    void updateStock_notFound_throwsResourceNotFound() {
        when(inventoryRepository.findByProductId("prod-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.updateStock("prod-99", 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("prod-99");
        verify(inventoryRepository, never()).save(inventory);
    }

    @Test
    void deductStock_notFound_throwsResourceNotFound() {
        when(inventoryRepository.findByProductId("prod-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.deductStock("prod-99", 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("prod-99");
        verify(inventoryRepository, never()).save(inventory);
    }

    @Test
    void validateStock_notFound_throwsResourceNotFound() {
        when(inventoryRepository.findByProductId("prod-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.validateStock("prod-99", 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("prod-99");
    }

    @Test
    void deleteInventory_notFound_throwsResourceNotFound() {
        when(inventoryRepository.findByProductId("prod-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.deleteInventory("prod-99"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("prod-99");
        verify(inventoryRepository, never()).deleteByProductId("prod-99");
    }
}
