package com.deva.inventoryservice;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreamLambdaHandlerTest {

    @SuppressWarnings("unchecked")
    @Test
    void handleRequest_parsesRequestAndDelegatesToHandler() throws Exception {
        SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> mockHandler =
                mock(SpringBootLambdaContainerHandler.class);

        try (MockedStatic<SpringBootLambdaContainerHandler> mocked =
                     mockStatic(SpringBootLambdaContainerHandler.class)) {
            mocked.when(() -> SpringBootLambdaContainerHandler
                            .getHttpApiV2ProxyHandler(InventoryserviceApplication.class))
                    .thenReturn(mockHandler);

            StreamLambdaHandler lambda = new StreamLambdaHandler();
            AwsProxyResponse awsResponse = new AwsProxyResponse(200);
            when(mockHandler.proxy(any(HttpApiV2ProxyRequest.class), any(Context.class)))
                    .thenReturn(awsResponse);
            Context context = mock(Context.class);

            InputStream inputWithHeaders = new ByteArrayInputStream(
                    "{\"headers\":{\"authorization\":\"Bearer abc\"}}".getBytes());
            ByteArrayOutputStream outputWithHeaders = new ByteArrayOutputStream();

            lambda.handleRequest(inputWithHeaders, outputWithHeaders, context);

            ArgumentCaptor<HttpApiV2ProxyRequest> captor =
                    ArgumentCaptor.forClass(HttpApiV2ProxyRequest.class);
            verify(mockHandler).proxy(captor.capture(), eq(context));
            assertThat(captor.getValue().getHeaders()).containsEntry("authorization", "Bearer abc");
            assertThat(outputWithHeaders.size()).isPositive();

            InputStream inputWithoutHeaders = new ByteArrayInputStream("{}".getBytes());
            ByteArrayOutputStream outputWithoutHeaders = new ByteArrayOutputStream();

            lambda.handleRequest(inputWithoutHeaders, outputWithoutHeaders, context);

            verify(mockHandler, times(2)).proxy(captor.capture(), eq(context));
            assertThat(captor.getValue().getHeaders()).isNotNull();
            assertThat(captor.getValue().getHeaders()).isEmpty();
            assertThat(outputWithoutHeaders.size()).isPositive();
        }
    }

    @Test
    void staticInit_containerInitializationFailure_wrapsInRuntimeException() throws Exception {
        URL classesUrl = Paths.get("target/classes").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classesUrl}, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if ("com.deva.inventoryservice.StreamLambdaHandler".equals(name)) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) {
                            loaded = findClass(name);
                        }
                        if (resolve) {
                            resolveClass(loaded);
                        }
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        }) {
            try (MockedStatic<SpringBootLambdaContainerHandler> mocked =
                         mockStatic(SpringBootLambdaContainerHandler.class)) {
                mocked.when(() -> SpringBootLambdaContainerHandler
                                .getHttpApiV2ProxyHandler(InventoryserviceApplication.class))
                        .thenThrow(new ContainerInitializationException(
                                "failed", new Exception("boom")));

                assertThatThrownBy(() -> Class.forName(
                        "com.deva.inventoryservice.StreamLambdaHandler", true, loader))
                        .isInstanceOf(ExceptionInInitializerError.class)
                        .hasCauseInstanceOf(RuntimeException.class)
                        .hasStackTraceContaining("Could not initialize Spring Boot application");
            }
        }
    }
}
