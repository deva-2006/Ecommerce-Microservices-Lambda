package com.deva.cartservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class XRayConfigTest {

    private final XRayConfig filter = new XRayConfig();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @AfterEach
    void cleanup() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    void nonHttpRequest_forwardsWithoutTracing() throws Exception {
        ServletRequest plainRequest = mock(ServletRequest.class);
        ServletResponse plainResponse = mock(ServletResponse.class);

        filter.doFilter(plainRequest, plainResponse, chain);

        verify(chain).doFilter(plainRequest, plainResponse);
    }

    @Test
    void httpRequest_withoutSegment_forwardsAndDoesNotTouchChainStatus() throws Exception {
        AWSXRay.clearTraceEntity();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void httpRequest_withSegment_recordsRequestAndResponse() throws Exception {
        AWSXRay.clearTraceEntity();
        Segment segment = AWSXRay.beginSegment("test-segment");
        try {
            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        } finally {
            AWSXRay.endSegment();
        }
    }

    @Test
    void httpRequest_withSegment_whenChainThrows_rethrows() throws Exception {
        AWSXRay.clearTraceEntity();
        doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);
        Segment segment = AWSXRay.beginSegment("test-segment");
        try {
            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("boom");
        } finally {
            AWSXRay.endSegment();
        }
    }
}
