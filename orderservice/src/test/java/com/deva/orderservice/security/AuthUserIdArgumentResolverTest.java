package com.deva.orderservice.security;

import com.amazonaws.serverless.proxy.model.HttpApiV2AuthorizerMap;
import com.amazonaws.serverless.proxy.model.HttpApiV2JwtAuthorizer;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext;
import com.deva.orderservice.controller.OrderController;
import com.deva.orderservice.dto.OrderRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;
import java.util.Map;

import static com.amazonaws.serverless.proxy.RequestReader.HTTP_API_EVENT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserIdArgumentResolverTest {

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpApiV2ProxyRequest event;

    @Mock
    private HttpApiV2ProxyRequestContext requestContext;

    @Mock
    private HttpApiV2AuthorizerMap authorizer;

    @Mock
    private HttpApiV2JwtAuthorizer jwtAuthorizer;

    private final AuthUserIdArgumentResolver resolver = new AuthUserIdArgumentResolver();

    private MethodParameter authUserIdParam;
    private MethodParameter plainStringParam;
    private MethodParameter nonStringAnnotatedParam;

    static class DummyHandler {
        public void handle(@AuthUserId Integer userId) {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        Method createOrder = OrderController.class.getMethod("createOrder", String.class, OrderRequestDTO.class);
        authUserIdParam = new MethodParameter(createOrder, 0);
        Method getOrderById = OrderController.class.getMethod("getOrderById", String.class);
        plainStringParam = new MethodParameter(getOrderById, 0);
        nonStringAnnotatedParam = new MethodParameter(DummyHandler.class.getMethod("handle", Integer.class), 0);
    }

    @Test
    void supportsParameter_trueForAnnotatedString() {
        assertThat(resolver.supportsParameter(authUserIdParam)).isTrue();
    }

    @Test
    void supportsParameter_falseForPlainString() {
        assertThat(resolver.supportsParameter(plainStringParam)).isFalse();
    }

    @Test
    void supportsParameter_falseForAnnotatedNonString() {
        assertThat(resolver.supportsParameter(nonStringAnnotatedParam)).isFalse();
    }

    @Test
    void resolveArgument_noServletRequest_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        assertThatThrownBy(() -> resolve())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No servlet request available");
    }

    @Test
    void resolveArgument_missingApiGatewayEvent_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY)).thenReturn(null);

        assertThatThrownBy(() -> resolve())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing API Gateway event context");
    }

    @Test
    void resolveArgument_wrongEventType_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY)).thenReturn("not-an-event");

        assertThatThrownBy(() -> resolve())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing API Gateway event context");
    }

    @Test
    void resolveArgument_noRequestContext_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY)).thenReturn(event);
        when(event.getRequestContext()).thenReturn(null);

        assertThatThrownBy(() -> resolve())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_noAuthorizer_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY)).thenReturn(event);
        when(event.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getAuthorizer()).thenReturn(null);

        assertThatThrownBy(() -> resolve())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_noJwtAuthorizer_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY)).thenReturn(event);
        when(event.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getAuthorizer()).thenReturn(authorizer);
        when(authorizer.getJwtAuthorizer()).thenReturn(null);

        assertThatThrownBy(() -> resolve())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_nullClaims_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY)).thenReturn(event);
        when(event.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getAuthorizer()).thenReturn(authorizer);
        when(authorizer.getJwtAuthorizer()).thenReturn(jwtAuthorizer);
        when(jwtAuthorizer.getClaims()).thenReturn(null);

        assertThatThrownBy(() -> resolve())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_blankSub_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY)).thenReturn(event);
        when(event.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getAuthorizer()).thenReturn(authorizer);
        when(authorizer.getJwtAuthorizer()).thenReturn(jwtAuthorizer);
        when(jwtAuthorizer.getClaims()).thenReturn(Map.of("sub", "   "));

        assertThatThrownBy(() -> resolve())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    void resolveArgument_validSub_returnsUserId() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY)).thenReturn(event);
        when(event.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getAuthorizer()).thenReturn(authorizer);
        when(authorizer.getJwtAuthorizer()).thenReturn(jwtAuthorizer);
        when(jwtAuthorizer.getClaims()).thenReturn(Map.of("sub", "user-123"));

        Object result = resolve();

        assertThat(result).isEqualTo("user-123");
    }

    private Object resolve() {
        return resolver.resolveArgument(authUserIdParam, null, webRequest, null);
    }
}
