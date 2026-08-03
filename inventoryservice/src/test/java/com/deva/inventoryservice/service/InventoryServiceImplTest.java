package com.deva.inventoryservice.service;

import com.deva.inventoryservice.dto.InventoryRequestDTO;
import com.deva.inventoryservice.dto.InventoryResponseDTO;
import com.deva.inventoryservice.entity.Inventory;
import com.deva.inventoryservice.exception.ResourceNotFoundException;
import com.deva.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private InventoryRequestDTO requestDTO;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        requestDTO = new InventoryRequestDTO();
        requestDTO.setProductId("prod-10");
        requestDTO.setQuantity(50);

        inventory = Inventory.builder()
                .productId("prod-10")
                .quantity(50)
                .updatedAt("2026-08-01T12:00:00")
                .build();
    }

    @Test
    void createInventory_success() {
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        InventoryResponseDTO response = inventoryService.createInventory(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo("prod-10");
        assertThat(response.getQuantity()).isEqualTo(50);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void getInventoryByProductId_success() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.of(inventory));

        InventoryResponseDTO response = inventoryService.getInventoryByProductId("prod-10");

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo("prod-10");
    }

    @Test
    void getInventoryByProductId_notFound_throwsException() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryByProductId("prod-10"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllInventory_success() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        List<InventoryResponseDTO> list = inventoryService.getAllInventory();

        assertThat(list).hasSize(1);
    }

    @Test
    void addStock_success() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        InventoryResponseDTO response = inventoryService.addStock("prod-10", 20);

        assertThat(response).isNotNull();
        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    void updateStock_success() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        InventoryResponseDTO response = inventoryService.updateStock("prod-10", 100);

        assertThat(response).isNotNull();
        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    void deductStock_success() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.deductStock("prod-10", 30);

        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    void deductStock_insufficientStock_throwsIllegalState() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.deductStock("prod-10", 100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void validateStock_success() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.of(inventory));

        inventoryService.validateStock("prod-10", 30);
    }

    @Test
    void validateStock_insufficientStock_throwsIllegalState() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.validateStock("prod-10", 100))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteInventory_success() {
        when(inventoryRepository.findByProductId("prod-10")).thenReturn(Optional.of(inventory));
        doNothing().when(inventoryRepository).deleteByProductId("prod-10");

        inventoryService.deleteInventory("prod-10");

        verify(inventoryRepository, times(1)).deleteByProductId("prod-10");
    }
}
