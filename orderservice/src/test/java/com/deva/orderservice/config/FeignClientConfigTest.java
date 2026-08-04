package com.deva.orderservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeignClientConfigTest {

    private final FeignClientConfig config = new FeignClientConfig();

    @Test
    void interceptor_noRequestAttributes_addsNoHeader() {
        RequestContextHolder.resetRequestAttributes();

        RequestInterceptor interceptor = config.authHeaderForwardingInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void interceptor_withAuthorizationHeader_forwardsIt() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            RequestInterceptor interceptor = config.authHeaderForwardingInterceptor();
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            assertThat(template.headers().get("Authorization")).contains("Bearer token-123");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void interceptor_withoutAuthorizationHeader_addsNothing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            RequestInterceptor interceptor = config.authHeaderForwardingInterceptor();
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            assertThat(template.headers()).doesNotContainKey("Authorization");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
