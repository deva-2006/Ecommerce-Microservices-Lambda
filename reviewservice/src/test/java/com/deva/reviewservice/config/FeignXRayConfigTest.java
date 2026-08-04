package com.deva.reviewservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceID;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class FeignXRayConfigTest {

    private final FeignXRayConfig config = new FeignXRayConfig();

    @Test
    void feignTracingInterceptor_addsTraceHeaderWhenSegmentPresent() {
        Segment segment = mock(Segment.class);
        TraceID traceId = mock(TraceID.class);
        when(segment.getTraceId()).thenReturn(traceId);
        when(segment.isSampled()).thenReturn(true);
        when(segment.getId()).thenReturn("53995c3f42cd8ad8");

        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(AWSXRay::getCurrentSegmentOptional).thenReturn(Optional.of(segment));

            RequestInterceptor interceptor = config.feignTracingInterceptor();
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            assertThat(template.headers()).containsKey("X-Amzn-Trace-Id");
        }
    }

    @Test
    void feignTracingInterceptor_doesNotAddHeaderWhenNoSegment() {
        try (MockedStatic<AWSXRay> xray = mockStatic(AWSXRay.class)) {
            xray.when(AWSXRay::getCurrentSegmentOptional).thenReturn(Optional.empty());

            RequestInterceptor interceptor = config.feignTracingInterceptor();
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            assertThat(template.headers()).doesNotContainKey("X-Amzn-Trace-Id");
        }
    }
}
