package com.deva.productservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DynamoDbConfigTest {

    @InjectMocks
    private DynamoDbConfig dynamoDbConfig;

    @Test
    void dynamoDbEnhancedClient_shouldReturnNonNull() {
        ReflectionTestUtils.setField(dynamoDbConfig, "region", "us-east-1");
        DynamoDbClient mockClient = mock(DynamoDbClient.class);

        DynamoDbEnhancedClient result = dynamoDbConfig.dynamoDbEnhancedClient(mockClient);

        assertThat(result).isNotNull();
    }

    @Test
    void dynamoDbEnhancedClient_shouldAcceptClient() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);

        DynamoDbEnhancedClient result = dynamoDbConfig.dynamoDbEnhancedClient(mockClient);

        assertThat(result).isInstanceOf(DynamoDbEnhancedClient.class);
    }

    @Test
    void dynamoDbClient_shouldBuildWithRegion() {
        ReflectionTestUtils.setField(dynamoDbConfig, "region", "us-east-1");

        DynamoDbClient result = dynamoDbConfig.dynamoDbClient();

        assertThat(result).isNotNull();
        result.close();
    }
}
