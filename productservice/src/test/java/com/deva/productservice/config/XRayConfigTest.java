package com.deva.productservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XRayConfigTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private Subsegment subsegment;

    private final XRayConfig config = new XRayConfig();

    @Test
    void doFilter_nonHttpRequest_delegatesDirectly() throws Exception {
        ServletRequest nonHttp = mock(ServletRequest.class);
        ServletResponse nonHttpResponse = mock(ServletResponse.class);

        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            config.doFilter(nonHttp, nonHttpResponse, chain);

            verify(chain).doFilter(nonHttp, nonHttpResponse);
            xray.verifyNoInteractions();
        }
    }

    @Test
    void doFilter_httpWithSubsegment_recordsRequestAndResponse() throws Exception {
        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(() -> AWSXRay.beginSubsegment("product-service")).thenReturn(subsegment);
            when(request.getRequestURI()).thenReturn("/api/products");
            when(request.getMethod()).thenReturn("GET");
            when(response.getStatus()).thenReturn(200);

            config.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verify(subsegment).putHttp(eq("request"), any());
            verify(subsegment).putHttp(eq("response"), any());
            xray.verify(() -> AWSXRay.endSubsegment());
        }
    }

    @Test
    void doFilter_httpWithoutSubsegment_stillDelegates() throws Exception {
        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(() -> AWSXRay.beginSubsegment("product-service")).thenReturn(null);

            config.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            xray.verify(() -> AWSXRay.endSubsegment(), never());
        }
    }

    @Test
    void doFilter_exception_addsExceptionAndRethrows() throws Exception {
        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(() -> AWSXRay.beginSubsegment("product-service")).thenReturn(subsegment);
            doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

            assertThatThrownBy(() -> config.doFilter(request, response, chain))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("boom");

            verify(subsegment).addException(any(RuntimeException.class));
            xray.verify(() -> AWSXRay.endSubsegment());
        }
    }

    @Test
    void doFilter_exceptionWithoutSubsegment_rethrowsWithoutSubsegmentCalls() throws Exception {
        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(() -> AWSXRay.beginSubsegment("product-service")).thenReturn(null);
            doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

            assertThatThrownBy(() -> config.doFilter(request, response, chain))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("boom");

            xray.verify(() -> AWSXRay.endSubsegment(), never());
        }
    }
}
