package com.deva.reviewservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

class UnauthorizedExceptionTest {

    @Test
    void exceptionCarriesMessage() {
        UnauthorizedException ex = new UnauthorizedException("Access denied");

        assertThat(ex.getMessage()).isEqualTo("Access denied");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void annotatedWithUnauthorizedResponseStatus() {
        ResponseStatus annotation = UnauthorizedException.class.getAnnotation(ResponseStatus.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
