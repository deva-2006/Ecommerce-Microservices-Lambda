package com.deva.productservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    void webConfig_shouldImplementWebMvcConfigurer() {
        WebConfig webConfig = new WebConfig();
        assertThat(webConfig).isInstanceOf(WebMvcConfigurer.class);
    }

    @SuppressWarnings("unchecked")
    private CorsConfiguration getCorsConfig(WebConfig webConfig, String pattern) throws Exception {
        CorsRegistry registry = new CorsRegistry();
        webConfig.addCorsMappings(registry);

        Method getCorsConfigurations = CorsRegistry.class.getDeclaredMethod("getCorsConfigurations");
        getCorsConfigurations.setAccessible(true);
        Map<String, CorsConfiguration> configs =
                (Map<String, CorsConfiguration>) getCorsConfigurations.invoke(registry);
        return configs.get(pattern);
    }

    @Test
    void addCorsMappings_shouldConfigureMappings() throws Exception {
        WebConfig webConfig = new WebConfig();
        CorsConfiguration config = getCorsConfig(webConfig, "/**");

        assertThat(config).isNotNull();
    }

    @Test
    void addCorsMappings_shouldSetMaxAgeTo3600() throws Exception {
        WebConfig webConfig = new WebConfig();
        CorsConfiguration config = getCorsConfig(webConfig, "/**");

        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void addCorsMappings_shouldAllowMultipleMethods() throws Exception {
        WebConfig webConfig = new WebConfig();
        CorsConfiguration config = getCorsConfig(webConfig, "/**");

        assertThat(config.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void addCorsMappings_shouldAllowAllHeaders() throws Exception {
        WebConfig webConfig = new WebConfig();
        CorsConfiguration config = getCorsConfig(webConfig, "/**");

        assertThat(config.getAllowedHeaders()).containsExactly("*");
    }

    @Test
    void addCorsMappings_shouldConfigureLocalhostOrigin() throws Exception {
        WebConfig webConfig = new WebConfig();
        CorsConfiguration config = getCorsConfig(webConfig, "/**");

        assertThat(config.getAllowedOrigins()).contains("http://localhost:5173");
    }

    @Test
    void addCorsMappings_shouldConfigureCloudfrontOrigin() throws Exception {
        WebConfig webConfig = new WebConfig();
        CorsConfiguration config = getCorsConfig(webConfig, "/**");

        assertThat(config.getAllowedOrigins()).contains("https://dhvfhexmyhpvv.cloudfront.net");
    }
}
