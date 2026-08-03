package com.deva.orderservice.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
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

        Subsegment subsegment = AWSXRay.beginSubsegment("order-service");
        try {
            if (subsegment != null) {
                Map<String, Object> reqData = new HashMap<>();
                reqData.put("url", httpRequest.getRequestURI());
                reqData.put("method", httpRequest.getMethod());
                subsegment.putHttp("request", reqData);
            }

            chain.doFilter(request, response);

            if (subsegment != null) {
                Map<String, Object> resData = new HashMap<>();
                resData.put("status", httpResponse.getStatus());
                subsegment.putHttp("response", resData);
            }
        } catch (Exception e) {
            if (subsegment != null) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            if (subsegment != null) {
                AWSXRay.endSubsegment();
            }
        }
    }
}
