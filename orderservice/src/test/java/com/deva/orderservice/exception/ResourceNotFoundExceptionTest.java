package com.deva.orderservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_storesMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Order not found");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Order not found");
    }
}
