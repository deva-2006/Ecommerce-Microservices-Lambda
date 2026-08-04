package com.deva.paymentservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;
import com.deva.paymentservice.security.AuthUserArgumentResolver;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigBeansTest {

    @Test
    void cognitoConfig_createsClient() {
        CognitoConfig config = new CognitoConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");

        assertThat(config.cognitoClient()).isInstanceOf(CognitoIdentityProviderClient.class);
    }

    @Test
    void snsConfig_createsClient() {
        SnsConfig config = new SnsConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");

        assertThat(config.snsClient()).isInstanceOf(SnsClient.class);
    }

    @Test
    void dynamoDbConfig_createsClients() {
        DynamoDbConfig config = new DynamoDbConfig();
        ReflectionTestUtils.setField(config, "region", "us-east-1");

        DynamoDbClient client = config.dynamoDbClient();

        assertThat(client).isNotNull();
        assertThat(config.dynamoDbEnhancedClient(client)).isNotNull();
    }

    @Test
    void authHeaderForwardingInterceptor_noRequestContext_doesNothing() {
        FeignClientConfig config = new FeignClientConfig();
        RequestInterceptor interceptor = config.authHeaderForwardingInterceptor();
        RequestTemplate template = new RequestTemplate();
        RequestContextHolder.resetRequestAttributes();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void authHeaderForwardingInterceptor_forwardsAuthorizationHeader() {
        FeignClientConfig config = new FeignClientConfig();
        RequestInterceptor interceptor = config.authHeaderForwardingInterceptor();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();
        try {
            interceptor.apply(template);

            assertThat(template.headers().get("Authorization")).containsExactly("Bearer abc123");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void feignTracingInterceptor_noSegment_doesNothing() {
        FeignXRayConfig config = new FeignXRayConfig();
        RequestInterceptor interceptor = config.feignTracingInterceptor();
        RequestTemplate template = new RequestTemplate();

        try (var xray = mockStatic(AWSXRay.class)) {
            when(AWSXRay.getCurrentSegmentOptional()).thenReturn(Optional.empty());

            interceptor.apply(template);
        }

        assertThat(template.headers()).doesNotContainKey("X-Amzn-Trace-Id");
    }

    @Test
    void feignTracingInterceptor_withSegment_addsTraceHeader() {
        FeignXRayConfig config = new FeignXRayConfig();
        RequestInterceptor interceptor = config.feignTracingInterceptor();
        RequestTemplate template = new RequestTemplate();
        Segment segment = mock(Segment.class);
        TraceHeader traceHeader = mock(TraceHeader.class);

        try (var xray = mockStatic(AWSXRay.class);
             var headerStatics = mockStatic(TraceHeader.class)) {
            when(AWSXRay.getCurrentSegmentOptional()).thenReturn(Optional.of(segment));
            when(TraceHeader.fromEntity(segment)).thenReturn(traceHeader);
            when(traceHeader.toString()).thenReturn("Root=1-abcdef;Parent=2;Sampled=1");

            interceptor.apply(template);
        }

        assertThat(template.headers().get("X-Amzn-Trace-Id")).containsExactly("Root=1-abcdef;Parent=2;Sampled=1");
    }

    @Test
    void xRayFilter_nonHttpRequest_delegatesToChain() throws Exception {
        XRayConfig filter = new XRayConfig();
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void xRayFilter_nullSubsegment_delegatesToChain() throws Exception {
        XRayConfig filter = new XRayConfig();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        try (var xray = mockStatic(AWSXRay.class)) {
            when(AWSXRay.beginSubsegment("payment-service")).thenReturn(null);

            filter.doFilter(request, response, chain);
        }

        verify(chain).doFilter(request, response);
    }

    @Test
    void xRayFilter_withSubsegment_recordsRequestAndResponse() throws Exception {
        XRayConfig filter = new XRayConfig();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        Subsegment subsegment = mock(Subsegment.class);

        try (var xray = mockStatic(AWSXRay.class)) {
            when(AWSXRay.beginSubsegment("payment-service")).thenReturn(subsegment);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verify(subsegment).putHttp(eq("request"), anyMap());
            verify(subsegment).putHttp(eq("response"), anyMap());
            xray.verify(AWSXRay::endSubsegment);
        }
    }

    @Test
    void xRayFilter_exception_recordsAndRethrows() throws Exception {
        XRayConfig filter = new XRayConfig();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        Subsegment subsegment = mock(Subsegment.class);
        doThrow(new IOException("boom")).when(chain).doFilter(any(), any());

        try (var xray = mockStatic(AWSXRay.class)) {
            when(AWSXRay.beginSubsegment("payment-service")).thenReturn(subsegment);

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(IOException.class)
                    .hasMessage("boom");

            verify(subsegment).addException(any());
            xray.verify(AWSXRay::endSubsegment);
        }
    }

    @Test
    void webConfig_addsCorsMappings() {
        new WebConfig().addCorsMappings(new CorsRegistry());
    }

    @Test
    void webConfig_registersAuthUserArgumentResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        new WebConfig().addArgumentResolvers(resolvers);

        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0)).isInstanceOf(AuthUserArgumentResolver.class);
    }
}
