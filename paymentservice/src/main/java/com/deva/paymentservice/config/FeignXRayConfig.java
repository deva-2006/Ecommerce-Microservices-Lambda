package com.deva.paymentservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.TraceHeader;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignXRayConfig {

    @Bean
    public RequestInterceptor feignTracingInterceptor() {
        return (RequestTemplate template) -> {
            AWSXRay.getCurrentSegmentOptional().ifPresent(segment -> {
                TraceHeader traceHeader = new TraceHeader(segment.getTraceId(), segment.getId(), null);
                template.header("X-Amzn-Trace-Id", traceHeader.toString());
            });
        };
    }
}
