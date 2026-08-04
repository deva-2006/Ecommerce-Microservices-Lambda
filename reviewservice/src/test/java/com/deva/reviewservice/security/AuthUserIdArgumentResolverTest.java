package com.deva.reviewservice.security;

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.HttpApiV2AuthorizerMap;
import com.amazonaws.serverless.proxy.model.HttpApiV2JwtAuthorizer;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserIdArgumentResolverTest {

    private static class SampleController {
        public void annotatedString(@AuthUserId String userId) {
        }

        public void annotatedInt(@AuthUserId int userId) {
        }

        public void plainString(String userId) {
        }
    }

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private ModelAndViewContainer mavContainer;

    @Mock
    private WebDataBinderFactory binderFactory;

    @Mock
    private HttpServletRequest servletRequest;

    private AuthUserIdArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthUserIdArgumentResolver();
    }

    @Test
    void supportsParameter_annotatedString_returnsTrue() throws Exception {
        MethodParameter parameter = parameter("annotatedString", 0);

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    void supportsParameter_noAnnotation_returnsFalse() throws Exception {
        MethodParameter parameter = parameter("plainString", 0);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    void supportsParameter_annotatedNonString_returnsFalse() throws Exception {
        MethodParameter parameter = parameter("annotatedInt", 0);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    void resolveArgument_noServletRequest_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter("annotatedString", 0), mavContainer, webRequest, binderFactory))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No servlet request available");
    }

    @Test
    void resolveArgument_noApiGatewayEvent_usesDevHeader() throws Exception {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY)).thenReturn("not-an-event");
        when(servletRequest.getHeader("X-User-Id")).thenReturn("dev-user");

        Object result = resolver.resolveArgument(parameter("annotatedString", 0), mavContainer, webRequest, binderFactory);

        assertThat(result).isEqualTo("dev-user");
    }

    @Test
    void resolveArgument_noApiGatewayEvent_blankDevHeader_throwsUnauthorized() {
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY)).thenReturn("not-an-event");
        when(servletRequest.getHeader("X-User-Id")).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter("annotatedString", 0), mavContainer, webRequest, binderFactory))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing API Gateway event context");
    }

    @Test
    void resolveArgument_eventWithJwtSub_returnsSub() throws Exception {
        HttpApiV2ProxyRequest event = eventWithClaims(Map.of("sub", "user-123"));
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY)).thenReturn(event);

        Object result = resolver.resolveArgument(parameter("annotatedString", 0), mavContainer, webRequest, binderFactory);

        assertThat(result).isEqualTo("user-123");
    }

    @Test
    void resolveArgument_eventWithoutAuthorizer_throwsUnauthorized() {
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        event.setRequestContext(new HttpApiV2ProxyRequestContext());
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY)).thenReturn(event);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter("annotatedString", 0), mavContainer, webRequest, binderFactory))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found in JWT claims");
    }

    @Test
    void resolveArgument_eventWithBlankSub_throwsUnauthorized() {
        HttpApiV2ProxyRequest event = eventWithClaims(Map.of("sub", "   "));
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        when(servletRequest.getAttribute(RequestReader.HTTP_API_EVENT_PROPERTY)).thenReturn(event);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter("annotatedString", 0), mavContainer, webRequest, binderFactory))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found in JWT claims");
    }

    private HttpApiV2ProxyRequest eventWithClaims(Map<String, String> claims) {
        HttpApiV2JwtAuthorizer jwt = new HttpApiV2JwtAuthorizer();
        jwt.setClaims(claims);
        HttpApiV2AuthorizerMap authorizer = new HttpApiV2AuthorizerMap();
        authorizer.putJwtAuthorizer(jwt);
        HttpApiV2ProxyRequestContext context = new HttpApiV2ProxyRequestContext();
        context.setAuthorizer(authorizer);
        HttpApiV2ProxyRequest event = new HttpApiV2ProxyRequest();
        event.setRequestContext(context);
        return event;
    }

    private MethodParameter parameter(String methodName, int index) throws NoSuchMethodException {
        Class<?> paramType = methodName.equals("annotatedInt") ? int.class : String.class;
        Method method = SampleController.class.getMethod(methodName, paramType);
        return new MethodParameter(method, index);
    }
}
