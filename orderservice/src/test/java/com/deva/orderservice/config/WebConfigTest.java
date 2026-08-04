package com.deva.orderservice.config;

import com.deva.orderservice.security.AuthUserIdArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    private final WebConfig config = new WebConfig();

    @Test
    void addArgumentResolvers_registersAuthResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        config.addArgumentResolvers(resolvers);

        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0)).isInstanceOf(AuthUserIdArgumentResolver.class);
    }

    @Test
    void addCorsMappings_registersMappings() {
        config.addCorsMappings(new CorsRegistry());
    }
}
