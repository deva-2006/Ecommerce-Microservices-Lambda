package com.deva.reviewservice.config;

import com.deva.reviewservice.security.AuthUserIdArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    private final WebConfig webConfig = new WebConfig();

    @Test
    void addArgumentResolvers_registersAuthUserIdResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        webConfig.addArgumentResolvers(resolvers);

        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0)).isInstanceOf(AuthUserIdArgumentResolver.class);
    }
}
