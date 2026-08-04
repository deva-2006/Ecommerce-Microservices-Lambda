package com.deva.orderservice.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnauthorizedExceptionTest {

    @Test
    void constructor_storesMessage() {
        UnauthorizedException ex = new UnauthorizedException("Access denied");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Access denied");
    }
}
