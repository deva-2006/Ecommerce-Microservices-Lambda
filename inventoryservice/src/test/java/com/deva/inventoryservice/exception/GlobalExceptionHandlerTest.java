package com.deva.inventoryservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleNotFound(new ResourceNotFoundException("Item missing"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("Item missing");
    }

    @Test
    void handleIllegalState() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalState(new IllegalStateException("Stock low"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("error")).isEqualTo("Stock low");
    }

    @Test
    void handleGeneral() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGeneral(new Exception("System failure"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("error")).contains("System failure");
    }
}
