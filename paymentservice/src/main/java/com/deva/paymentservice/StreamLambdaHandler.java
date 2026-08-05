package com.deva.paymentservice;
// Auto-create SonarCloud project on main branch

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class StreamLambdaHandler implements RequestStreamHandler {
    private static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(PaymentserviceApplication.class);
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        HttpApiV2ProxyRequest request = objectMapper.readValue(input, HttpApiV2ProxyRequest.class);
        if (request.getHeaders() == null) {
            request.setHeaders(new HashMap<>());
        }
        AwsProxyResponse response = handler.proxy(request, context);
        objectMapper.writeValue(output, response);
    }
}
