package com.deva.orderservice.config;

import com.amazonaws.xray.AWSXRay;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class XRayConfigTest {

    private final XRayConfig filter = new XRayConfig();

    @AfterEach
    void tearDown() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    void doFilter_nonHttpRequest_passesThrough() throws Exception {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_httpRequest_recordsAndContinues() throws Exception {
        AWSXRay.beginSegment("test-segment");
        try {
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            when(request.getRequestURI()).thenReturn("/orders");
            when(request.getMethod()).thenReturn("GET");
            when(response.getStatus()).thenReturn(200);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        } finally {
            AWSXRay.endSegment();
        }
    }

    @Test
    void doFilter_exception_addsExceptionAndRethrows() throws Exception {
        AWSXRay.beginSegment("test-segment");
        try {
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            RuntimeException boom = new RuntimeException("chain failed");
            doThrow(boom).when(chain).doFilter(request, response);

            assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isSameAs(boom);
        } finally {
            AWSXRay.endSegment();
        }
    }
}
