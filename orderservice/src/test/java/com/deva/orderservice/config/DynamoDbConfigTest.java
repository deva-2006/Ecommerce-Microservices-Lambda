package com.deva.orderservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;

class DynamoDbConfigTest {

    private final DynamoDbConfig config = new DynamoDbConfig();

    @Test
    void dynamoDbClient_buildsClientWithConfiguredRegion() {
        ReflectionTestUtils.setField(config, "region", "us-east-1");

        DynamoDbClient client = config.dynamoDbClient();

        assertThat(client).isNotNull();
        assertThat(client.serviceName()).isEqualTo("dynamodb");
    }

    @Test
    void dynamoDbEnhancedClient_buildsEnhancedClient() {
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        DynamoDbClient client = config.dynamoDbClient();

        DynamoDbEnhancedClient enhancedClient = config.dynamoDbEnhancedClient(client);

        assertThat(enhancedClient).isNotNull();
    }
}
