package com.deva.cartservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeignXRayConfigTest {

    private final FeignXRayConfig config = new FeignXRayConfig();

    @Mock
    private RequestTemplate template;

    @AfterEach
    void cleanup() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    void interceptor_withSegment_addsTraceHeader() {
        RequestInterceptor interceptor = config.feignTracingInterceptor();
        Segment segment = AWSXRay.beginSegment("test-segment");
        try {
            interceptor.apply(template);

            verify(template).header(eq("X-Amzn-Trace-Id"), anyString());
        } finally {
            AWSXRay.endSegment();
        }
    }

    @Test
    void interceptor_withoutSegment_addsNoTraceHeader() {
        AWSXRay.clearTraceEntity();
        RequestInterceptor interceptor = config.feignTracingInterceptor();

        interceptor.apply(template);

        verify(template, never()).header(anyString(), any(String[].class));
    }
}
