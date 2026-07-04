package com.deva.cartservice.security;

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


    // provides @AuthUserId
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUserId.class)
                && parameter.getParameterType().equals(String.class);
    }

    //method is called only if supportsParameter() returned true.
    //finds the user ID and return it.
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        //Gets the http request which contains all request information, including the API Gateway event context.
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        if (servletRequest == null) { //checks whether the request exists
            throw new UnauthorizedException("No servlet request available");
        }

        //gets the AWS API Gateway event from the request
        Object attr = servletRequest.getAttribute(HTTP_API_EVENT_PROPERTY);

        // event exists or not
        if (!(attr instanceof HttpApiV2ProxyRequest event)) {
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