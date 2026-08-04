package com.deva.cartservice.security;

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.HttpApiV2AuthorizerMap;
import com.amazonaws.serverless.proxy.model.HttpApiV2JwtAuthorizer;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserIdArgumentResolverTest {

    private AuthUserIdArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthUserIdArgumentResolver();
    }

    @SuppressWarnings("unused")
    private void sampleHandler(@AuthUserId String userId, String plain, @AuthUserId Integer number) {
    }

    private MethodParameter parameter(int index) throws Exception {
        Method method = AuthUserIdArgumentResolverTest.class
                .getDeclaredMethod("sampleHandler", String.class, String.class, Integer.class);
        return MethodParameter.forExecutable(method, index);
    }

    @Test
    void supportsParameter_stringWithAnnotation_returnsTrue() throws Exception {
        assertThat(resolver.supportsParameter(parameter(0))).isTrue();
    }

    @Test
    void supportsParameter_stringWithoutAnnotation_returnsFalse() throws Exception {
        assertThat(resolver.supportsParameter(parameter(1))).isFalse();
    }

    @Test
    void supportsParameter_annotatedNonString_returnsFalse() throws Exception {
        assertThat(resolver.supportsParameter(parameter(2))).isFalse();
    }

    @Test
    void resolveArgument_noServletRequest_throwsUnauthorized() throws Exception {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter(0), null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No servlet request");
    }

    @Test
    void resolveArgument_missingApiGatewayEvent_throwsUnauthorized() throws Exception {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        HttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(RequestReader.HTTP_API_EVENT_PROPERTY, "not-an-event");
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter(0), null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing API Gateway event context");
    }

    @Test
    void resolveArgument_noRequestContext_throwsUnauthorized() throws Exception {
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        event.setRequestContext(null);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter(0), null, requestWith(event), null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_noJwtAuthorizer_throwsUnauthorized() throws Exception {
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        HttpApiV2ProxyRequestContext context = new HttpApiV2ProxyRequestContext();
        context.setAuthorizer(new HttpApiV2AuthorizerMap());
        event.setRequestContext(context);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter(0), null, requestWith(event), null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_noClaims_throwsUnauthorized() throws Exception {
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        HttpApiV2ProxyRequestContext context = new HttpApiV2ProxyRequestContext();
        HttpApiV2AuthorizerMap authorizer = new HttpApiV2AuthorizerMap();
        authorizer.putJwtAuthorizer(new HttpApiV2JwtAuthorizer());
        context.setAuthorizer(authorizer);
        event.setRequestContext(context);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter(0), null, requestWith(event), null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_blankSub_throwsUnauthorized() throws Exception {
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        HttpApiV2ProxyRequestContext context = new HttpApiV2ProxyRequestContext();
        HttpApiV2AuthorizerMap authorizer = new HttpApiV2AuthorizerMap();
        HttpApiV2JwtAuthorizer jwt = new HttpApiV2JwtAuthorizer();
        jwt.setClaims(Map.of("sub", "   "));
        authorizer.putJwtAuthorizer(jwt);
        context.setAuthorizer(authorizer);
        event.setRequestContext(context);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter(0), null, requestWith(event), null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_validSub_returnsUserId() throws Exception {
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        HttpApiV2ProxyRequestContext context = new HttpApiV2ProxyRequestContext();
        HttpApiV2AuthorizerMap authorizer = new HttpApiV2AuthorizerMap();
        HttpApiV2JwtAuthorizer jwt = new HttpApiV2JwtAuthorizer();
        jwt.setClaims(Map.of("sub", "user-123"));
        authorizer.putJwtAuthorizer(jwt);
        context.setAuthorizer(authorizer);
        event.setRequestContext(context);

        Object result = resolver.resolveArgument(parameter(0), null, requestWith(event), null);

        assertThat(result).isEqualTo("user-123");
    }

    private NativeWebRequest requestWith(HttpApiV2ProxyRequest event) {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        HttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(RequestReader.HTTP_API_EVENT_PROPERTY, event);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        return webRequest;
    }
}
