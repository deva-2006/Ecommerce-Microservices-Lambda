package com.deva.reviewservice.repository;

import com.deva.reviewservice.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReviewRepository {

    private final DynamoDbEnhancedClient enhancedClient;

    @Value("${aws.dynamodb.table.reviews:reviews}")
    private String tableName;

    private DynamoDbTable<Review> getTable() {
        return enhancedClient.table(tableName, TableSchema.fromBean(Review.class));
    }

    public void save(Review review) {
        getTable().putItem(review);
    }

    public Optional<Review> findById(String reviewId) {
        return Optional.ofNullable(getTable().getItem(r -> r.key(k -> k.partitionValue(reviewId))));
    }

    public List<Review> findByProductId(String productId) {
        return getTable().scan().items().stream()
                .filter(r -> productId.equals(r.getProductId()))
                .collect(Collectors.toList());
    }

    public List<Review> findByUserId(String userId) {
        return getTable().scan().items().stream()
                .filter(r -> userId.equals(r.getUserId()))
                .collect(Collectors.toList());
    }

    public void deleteById(String reviewId) {
        getTable().deleteItem(r -> r.key(k -> k.partitionValue(reviewId)));
    }
}
