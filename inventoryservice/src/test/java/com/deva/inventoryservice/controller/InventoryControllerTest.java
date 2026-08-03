package com.deva.inventoryservice.controller;

import com.deva.inventoryservice.dto.InventoryRequestDTO;
import com.deva.inventoryservice.dto.InventoryResponseDTO;
import com.deva.inventoryservice.dto.StockDeductRequestDTO;
import com.deva.inventoryservice.service.InventoryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private HealthController healthController;

    private InventoryRequestDTO requestDTO;
    private InventoryResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        healthController = new HealthController();

        requestDTO = new InventoryRequestDTO();
        requestDTO.setProductId("prod-55");
        requestDTO.setQuantity(100);

        responseDTO = InventoryResponseDTO.builder()
                .productId("prod-55")
                .quantity(100)
                .updatedAt("2026-08-01T10:00:00")
                .build();
    }

    @Test
    void createInventory_returnsCreated() {
        when(inventoryService.createInventory(any(InventoryRequestDTO.class))).thenReturn(responseDTO);

        ResponseEntity<InventoryResponseDTO> response = inventoryController.createInventory(requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getInventory_returnsOk() {
        when(inventoryService.getInventoryByProductId("prod-55")).thenReturn(responseDTO);

        ResponseEntity<InventoryResponseDTO> response = inventoryController.getInventory("prod-55");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAllInventory_returnsOk() {
        when(inventoryService.getAllInventory()).thenReturn(List.of(responseDTO));

        ResponseEntity<List<InventoryResponseDTO>> response = inventoryController.getAllInventory();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void addStock_returnsOk() {
        when(inventoryService.addStock("prod-55", 10)).thenReturn(responseDTO);

        ResponseEntity<InventoryResponseDTO> response = inventoryController.addStock("prod-55", 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateStock_returnsOk() {
        when(inventoryService.updateStock("prod-55", 200)).thenReturn(responseDTO);

        ResponseEntity<InventoryResponseDTO> response = inventoryController.updateStock("prod-55", 200);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deductStock_returnsNoContent() {
        StockDeductRequestDTO deductRequest = new StockDeductRequestDTO();
        deductRequest.setQuantity(5);
        doNothing().when(inventoryService).deductStock("prod-55", 5);

        ResponseEntity<Void> response = inventoryController.deductStock("prod-55", deductRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }


    @Test
    void validateStock_returnsOk() {
        doNothing().when(inventoryService).validateStock("prod-55", 5);

        ResponseEntity<Void> response = inventoryController.validateStock("prod-55", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteInventory_returnsNoContent() {
        doNothing().when(inventoryService).deleteInventory("prod-55");

        ResponseEntity<Void> response = inventoryController.deleteInventory("prod-55");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void health_returnsUp() {
        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
}
