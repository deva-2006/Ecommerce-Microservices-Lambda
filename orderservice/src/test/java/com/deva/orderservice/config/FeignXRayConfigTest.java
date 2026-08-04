package com.deva.orderservice.config;

import com.amazonaws.xray.AWSXRay;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeignXRayConfigTest {

    private final FeignXRayConfig config = new FeignXRayConfig();

    @AfterEach
    void tearDown() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    void interceptor_noSegmentInContext_addsNoHeader() {
        AWSXRay.clearTraceEntity();

        RequestInterceptor interceptor = config.feignTracingInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("X-Amzn-Trace-Id");
    }

    @Test
    void interceptor_withActiveSegment_addsTraceHeader() {
        AWSXRay.beginSegment("test-segment");
        try {
            RequestInterceptor interceptor = config.feignTracingInterceptor();
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            assertThat(template.headers()).containsKey("X-Amzn-Trace-Id");
            assertThat(template.headers().get("X-Amzn-Trace-Id")).isNotEmpty();
        } finally {
            AWSXRay.endSegment();
        }
    }
}
