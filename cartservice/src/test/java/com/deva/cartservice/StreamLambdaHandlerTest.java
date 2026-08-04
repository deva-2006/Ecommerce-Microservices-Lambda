package com.deva.cartservice;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreamLambdaHandlerTest {

    @Test
    void handleRequest_withoutHeaders_populatesHeadersAndProxies() throws Exception {
        assertHandleRequest("{ \"version\": \"2.0\", \"rawPath\": \"/health/cart\" }", true);
    }

    @Test
    void handleRequest_withHeaders_proxiesWithExistingHeaders() throws Exception {
        assertHandleRequest(
                "{ \"version\": \"2.0\", \"rawPath\": \"/health/cart\", \"headers\": { \"Content-Type\": \"application/json\" } }",
                false);
    }

    private void assertHandleRequest(String json, boolean headersWereNull) throws Exception {
        try (MockedStatic<SpringBootLambdaContainerHandler> mocked =
                     mockStatic(SpringBootLambdaContainerHandler.class)) {

            SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> mockHandler =
                    mock(SpringBootLambdaContainerHandler.class);
            mocked.when(() -> SpringBootLambdaContainerHandler
                    .getHttpApiV2ProxyHandler(CartserviceApplication.class))
                    .thenReturn(mockHandler);

            AwsProxyResponse response = new AwsProxyResponse(200);
            response.setBody("{\"status\":\"UP\"}");
            when(mockHandler.proxy(any(HttpApiV2ProxyRequest.class), any(Context.class))).thenReturn(response);

            StreamLambdaHandler handler = new StreamLambdaHandler();
            replaceStaticHandler(mockHandler);

            InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Context context = mock(Context.class);

            handler.handleRequest(input, output, context);

            ArgumentCaptor<HttpApiV2ProxyRequest> captor = ArgumentCaptor.forClass(HttpApiV2ProxyRequest.class);
            verify(mockHandler).proxy(captor.capture(), any(Context.class));
            assertThat(captor.getValue().getHeaders()).isNotNull();
            assertThat(output.toByteArray()).isNotEmpty();
            assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8)).contains("statusCode");
        }
    }

    @SuppressWarnings("unchecked")
    private void replaceStaticHandler(
            SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> mockHandler)
            throws Exception {
        java.lang.reflect.Field field = StreamLambdaHandler.class.getDeclaredField("handler");
        field.setAccessible(true);
        field.set(null, mockHandler);
    }
}
