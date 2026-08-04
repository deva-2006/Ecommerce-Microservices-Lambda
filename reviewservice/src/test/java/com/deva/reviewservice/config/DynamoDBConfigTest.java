package com.deva.reviewservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;

class DynamoDBConfigTest {

    private DynamoDBConfig config;

    @BeforeEach
    void setUp() {
        config = new DynamoDBConfig();
        ReflectionTestUtils.setField(config, "region", "us-east-1");
    }

    @Test
    void dynamoDbClient_buildsClientWithConfiguredRegion() {
        DynamoDbClient client = config.dynamoDbClient();

        assertThat(client).isNotNull();
        assertThat(client.serviceName()).isEqualTo("dynamodb");
    }

    @Test
    void dynamoDbEnhancedClient_wrapsGivenClient() {
        DynamoDbClient dynamoDbClient = org.mockito.Mockito.mock(DynamoDbClient.class);

        DynamoDbEnhancedClient enhancedClient = config.dynamoDbEnhancedClient(dynamoDbClient);

        assertThat(enhancedClient).isNotNull();
    }
}
