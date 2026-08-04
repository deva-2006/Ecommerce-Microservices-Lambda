package com.deva.reviewservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XRayConfigTest {

    private final XRayConfig filter = new XRayConfig();

    @Test
    void doFilter_nonHttpRequest_passesThroughWithoutTracing() throws Exception {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_httpRequest_tracesRequestAndResponse() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        Subsegment subsegment = mock(Subsegment.class);

        when(request.getRequestURI()).thenReturn("/reviews/1");
        when(request.getMethod()).thenReturn("GET");
        when(response.getStatus()).thenReturn(200);

        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(() -> AWSXRay.beginSubsegment("review-service")).thenReturn(subsegment);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verify(subsegment).putHttp(eq("request"), any());
            verify(subsegment).putHttp(eq("response"), any());
            xray.verify(AWSXRay::endSubsegment);
        }
    }

    @Test
    void doFilter_httpRequest_noSubsegment_skipsTracing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(() -> AWSXRay.beginSubsegment("review-service")).thenReturn(null);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            xray.verify(() -> AWSXRay.endSubsegment(), never());
        }
    }

    @Test
    void doFilter_chainThrows_recordsExceptionAndRethrows() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        Subsegment subsegment = mock(Subsegment.class);
        RuntimeException boom = new RuntimeException("boom");

        org.mockito.Mockito.doThrow(boom).when(chain).doFilter(request, response);

        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(() -> AWSXRay.beginSubsegment("review-service")).thenReturn(subsegment);

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("boom");

            verify(subsegment).addException(boom);
            xray.verify(AWSXRay::endSubsegment);
        }
    }
}
