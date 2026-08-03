package com.deva.productservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class XRayConfig implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Segment segment = AWSXRay.beginSegment("product-service");
        try {
            Map<String, Object> reqData = new HashMap<>();
            reqData.put("url", httpRequest.getRequestURI());
            reqData.put("method", httpRequest.getMethod());
            segment.putHttp("request", reqData);

            chain.doFilter(request, response);

            Map<String, Object> resData = new HashMap<>();
            resData.put("status", httpResponse.getStatus());
            segment.putHttp("response", resData);
        } catch (Exception e) {
            segment.addException(e);
            throw e;
        } finally {
            AWSXRay.endSegment();
        }
    }
}
