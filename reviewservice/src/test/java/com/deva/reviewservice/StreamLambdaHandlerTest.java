package com.deva.reviewservice;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class StreamLambdaHandlerTest {

    @SuppressWarnings("unchecked")
    @Test
    void handleRequest_delegatesToContainerHandler() throws Exception {
        SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> mockHandler =
                mock(SpringBootLambdaContainerHandler.class);

        try (MockedStatic<SpringBootLambdaContainerHandler> mocked =
                     mockStatic(SpringBootLambdaContainerHandler.class)) {
            mocked.when(() -> SpringBootLambdaContainerHandler
                            .getHttpApiV2ProxyHandler(ReviewServiceApplication.class))
                    .thenReturn(mockHandler);

            StreamLambdaHandler lambda = new StreamLambdaHandler();

            InputStream input = new ByteArrayInputStream("{}".getBytes());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Context context = mock(Context.class);

            lambda.handleRequest(input, output, context);

            verify(mockHandler).proxyStream(input, output, context);
        }
    }
}
