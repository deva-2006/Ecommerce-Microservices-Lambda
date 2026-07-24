package com.deva.reviewservice.security;

import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2AuthorizerMap;
import com.amazonaws.serverless.proxy.model.HttpApiV2JwtAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.amazonaws.serverless.proxy.RequestReader.HTTP_API_EVENT_PROPERTY;

public class AuthUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUserId.class)
                && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        if (servletRequest == null) {
            throw new UnauthorizedException("No servlet request available");
        }

        Object attr = servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY);
        if (!(attr instanceof HttpApiV2ProxyRequest event)) {
            // Local dev fallback header
            String devUserId = servletRequest.getHeader("X-User-Id");
            if (devUserId != null && !devUserId.isBlank()) {
                return devUserId;
            }
            throw new UnauthorizedException("Missing API Gateway event context");
        }

        HttpApiV2AuthorizerMap authorizer = event.getRequestContext() != null
                ? event.getRequestContext().getAuthorizer() : null;
        HttpApiV2JwtAuthorizer jwtAuthorizer = authorizer != null ? authorizer.getJwtAuthorizer() : null;
        String sub = jwtAuthorizer != null && jwtAuthorizer.getClaims() != null
                ? jwtAuthorizer.getClaims().get("sub") : null;

        if (sub == null || sub.isBlank()) {
            throw new UnauthorizedException("No authenticated user found in JWT claims");
        }
        return sub;
    }
}
