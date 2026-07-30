package com.deva.cartservice.repository;

import com.deva.cartservice.entity.Cart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CartRepository {


    private final DynamoDbTable<Cart> cartTable;

    public CartRepository(DynamoDbEnhancedClient enhancedClient,
                          @Value("${aws.dynamodb.tableName}") String tableName) {
        this.cartTable = enhancedClient.table(tableName, TableSchema.fromBean(Cart.class));
    }

    public Cart save(Cart cart) {
        cartTable.putItem(cart);
        return cart;
    }

    public List<Cart> findByUserId(String userId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(userId).build());
        return cartTable.query(queryConditional)
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    public Optional<Cart> findByUserIdAndProductId(String userId, String productId) {
        Key key = Key.builder()
                .partitionValue(userId)
                .sortValue(productId)
                .build();
        return Optional.ofNullable(cartTable.getItem(key));
    }
    public void deleteByUserIdAndProductId(String userId, String productId) {
        Key key = Key.builder()
                .partitionValue(userId)
                .sortValue(productId)
                .build();
        cartTable.deleteItem(key);
    }

    public void deleteAllByUserId(String userId) {
        List<Cart> items = findByUserId(userId);
        items.forEach(item -> deleteByUserIdAndProductId(item.getUserId(), item.getProductId()));
    }
}