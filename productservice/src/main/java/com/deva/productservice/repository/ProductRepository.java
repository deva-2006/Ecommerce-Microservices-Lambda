package com.deva.productservice.repository;

import com.deva.productservice.entity.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProductRepository {
    // It is used to perform operations like getItem(), putItem(), updateItem(), and deleteItem() on that table.
    private final DynamoDbTable<Product> table;

    // Connects the table variable to the actual DynamoDB table.
    public ProductRepository(DynamoDbEnhancedClient enhancedClient,
                             @Value("${aws.dynamodb.table-name}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Product.class));
    }

    public void save(Product product) {
        table.putItem(product);
    }

    public Optional<Product> findById(String productId) {
        // Represents the primary key
        Key key = Key.builder().partitionValue(productId).build();
        // If a product exists, put it inside an Optional. If it's null, create an empty Optional
        return Optional.ofNullable(table.getItem(key));
    }

    public List<Product> findAll() {
        return table.scan().items().stream().toList();
    }

    public void delete(String productId) {
        Key key = Key.builder().partitionValue(productId).build();
        table.deleteItem(key);
    }
}
