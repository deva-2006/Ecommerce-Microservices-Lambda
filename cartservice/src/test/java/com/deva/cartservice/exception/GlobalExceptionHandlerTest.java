package com.deva.cartservice.exception;

import com.deva.cartservice.security.UnauthorizedException;
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
    void handleNotFound() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleNotFound(new ResourceNotFoundException("Not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("Not found");
    }

    @Test
    void handleIllegalState() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalState(new IllegalStateException("State error"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("error")).isEqualTo("State error");
    }

    @Test
    void handleIllegalArgument() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalArgument(new IllegalArgumentException("Invalid argument"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("Invalid argument");
    }

    @Test
    void handleServiceUnavailable() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleServiceUnavailable(new ServiceUnavailableException("Service down"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().get("error")).isEqualTo("Service down");
    }

    @Test
    void handleFeignClient() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleFeignClient(new FeignClientException(403, "Forbidden"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("error")).isEqualTo("Forbidden");
    }

    @Test
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "quantity", "must be positive");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("quantity")).isEqualTo("must be positive");
    }

    @Test
    void handleGeneral() {
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGeneral(new Exception("Unknown failure"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("error")).contains("Unknown failure");
    }

    @Test
    void handleUnauthorized() {
        ResponseEntity<String> response = exceptionHandler.handleUnauthorized(new UnauthorizedException("Access denied"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Access denied");
    }
}
