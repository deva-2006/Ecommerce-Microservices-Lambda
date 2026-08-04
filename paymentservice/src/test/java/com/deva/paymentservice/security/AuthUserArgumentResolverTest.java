package com.deva.paymentservice.security;

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.HttpApiV2AuthorizerMap;
import com.amazonaws.serverless.proxy.model.HttpApiV2JwtAuthorizer;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthUserArgumentResolverTest {

    private AuthUserArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthUserArgumentResolver();
    }

    @Test
    void supportsParameter_annotatedString_returnsTrue() throws Exception {
        assertThat(resolver.supportsParameter(annotatedStringParam())).isTrue();
    }

    @Test
    void supportsParameter_unannotated_returnsFalse() throws Exception {
        assertThat(resolver.supportsParameter(plainParam())).isFalse();
    }

    @Test
    void supportsParameter_annotatedNonString_returnsFalse() throws Exception {
        assertThat(resolver.supportsParameter(annotatedIntegerParam())).isFalse();
    }

    @Test
    void resolveArgument_success_returnsSubClaim() throws Exception {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY)).thenReturn(buildEvent("user-123"));
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);

        Object result = resolver.resolveArgument(annotatedStringParam(), null, webRequest, null);

        assertThat(result).isEqualTo("user-123");
    }

    @Test
    void resolveArgument_noServletRequest_throwsUnauthorized() throws Exception {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(annotatedStringParam(), null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No servlet request available");
    }

    @Test
    void resolveArgument_missingApiGatewayEvent_throwsUnauthorized() throws Exception {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY)).thenReturn("not-an-event");
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);

        assertThatThrownBy(() -> resolver.resolveArgument(annotatedStringParam(), null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing API Gateway event context");
    }

    @Test
    void resolveArgument_nullRequestContext_throwsUnauthorized() throws Exception {
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        event.setRequestContext(null);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY)).thenReturn(event);
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);

        assertThatThrownBy(() -> resolver.resolveArgument(annotatedStringParam(), null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing JWT claim: sub");
    }

    @Test
    void resolveArgument_noJwtAuthorizer_throwsUnauthorized() throws Exception {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY))
                .thenReturn(buildEventWithAuthorizer(new HttpApiV2AuthorizerMap()));
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);

        assertThatThrownBy(() -> resolver.resolveArgument(annotatedStringParam(), null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing JWT claim: sub");
    }

    @Test
    void resolveArgument_missingSubClaim_throwsUnauthorized() throws Exception {
        HttpApiV2JwtAuthorizer jwt = new HttpApiV2JwtAuthorizer();
        jwt.setClaims(Map.of("scope", "read"));
        HttpApiV2AuthorizerMap authorizer = new HttpApiV2AuthorizerMap();
        authorizer.putJwtAuthorizer(jwt);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY))
                .thenReturn(buildEventWithAuthorizer(authorizer));
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);

        assertThatThrownBy(() -> resolver.resolveArgument(annotatedStringParam(), null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing JWT claim: sub");
    }

    @Test
    void resolveArgument_blankSubClaim_throwsUnauthorized() throws Exception {
        HttpApiV2JwtAuthorizer jwt = new HttpApiV2JwtAuthorizer();
        jwt.setClaims(Map.of("sub", "   "));
        HttpApiV2AuthorizerMap authorizer = new HttpApiV2AuthorizerMap();
        authorizer.putJwtAuthorizer(jwt);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY))
                .thenReturn(buildEventWithAuthorizer(authorizer));
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);

        assertThatThrownBy(() -> resolver.resolveArgument(annotatedStringParam(), null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing JWT claim: sub");
    }

    private static HttpApiV2ProxyRequest buildEvent(String sub) {
        HttpApiV2JwtAuthorizer jwt = new HttpApiV2JwtAuthorizer();
        jwt.setClaims(Map.of("sub", sub));
        HttpApiV2AuthorizerMap authorizer = new HttpApiV2AuthorizerMap();
        authorizer.putJwtAuthorizer(jwt);
        return buildEventWithAuthorizer(authorizer);
    }

    private static HttpApiV2ProxyRequest buildEventWithAuthorizer(HttpApiV2AuthorizerMap authorizer) {
        HttpApiV2ProxyRequestContext requestContext = new HttpApiV2ProxyRequestContext();
        requestContext.setAuthorizer(authorizer);
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        event.setRequestContext(requestContext);
        return event;
    }

    @SuppressWarnings("unused")
    private static void resolverMethod(String plain, @AuthUserId String userId, @AuthUserId Integer number) {
    }

    private static MethodParameter annotatedStringParam() throws Exception {
        return MethodParameter.forExecutable(resolverMethodRef(), 1);
    }

    private static MethodParameter plainParam() throws Exception {
        return MethodParameter.forExecutable(resolverMethodRef(), 0);
    }

    private static MethodParameter annotatedIntegerParam() throws Exception {
        return MethodParameter.forExecutable(resolverMethodRef(), 2);
    }

    private static Method resolverMethodRef() throws Exception {
        return AuthUserArgumentResolverTest.class.getDeclaredMethod("resolverMethod", String.class, String.class, Integer.class);
    }
}
