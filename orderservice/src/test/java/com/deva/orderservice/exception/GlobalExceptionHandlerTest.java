package com.deva.orderservice.exception;

import com.deva.orderservice.security.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleUnauthorized() {
        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleUnauthorized(new UnauthorizedException("Access denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("error")).isEqualTo("Access denied");
    }

    @Test
    void handleNotFound() {
        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleNotFound(new ResourceNotFoundException("Order not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("Order not found");
    }

    @Test
    void handleBadRequest_illegalArgument() {
        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleBadRequest(new IllegalArgumentException("Invalid argument"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("Invalid argument");
    }

    @Test
    void handleBadRequest_illegalState() {
        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleBadRequest(new IllegalStateException("State error"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("State error");
    }

    @Test
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("order", "shippingAddress", "shippingAddress is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("shippingAddress")).isEqualTo("shippingAddress is required");
    }

    @Test
    void handleGeneral() {
        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleGeneral(new Exception("Unknown failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("error")).isEqualTo("Internal server error: Unknown failure");
    }
}
