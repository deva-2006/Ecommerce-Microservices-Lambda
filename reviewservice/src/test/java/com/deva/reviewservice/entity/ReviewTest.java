package com.deva.reviewservice.entity;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewTest {

    @Test
    void noArgsConstructor_andSetters() {
        Review review = new Review();
        review.setReviewId("rev-1");
        review.setProductId("prod-1");
        review.setUserId("user-1");
        review.setUserName("Alice");
        review.setRating(5);
        review.setComment("Great");
        review.setVerifiedBuyer(true);
        review.setCreatedAt("2026-08-01T10:00:00Z");

        assertThat(review.getReviewId()).isEqualTo("rev-1");
        assertThat(review.getProductId()).isEqualTo("prod-1");
        assertThat(review.getUserId()).isEqualTo("user-1");
        assertThat(review.getUserName()).isEqualTo("Alice");
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Great");
        assertThat(review.getVerifiedBuyer()).isTrue();
        assertThat(review.getCreatedAt()).isEqualTo("2026-08-01T10:00:00Z");
    }

    @Test
    void allArgsConstructor_setsFields() {
        Review review = new Review("rev-1", "prod-1", "user-1", "Alice", 5, "Great", true, "ts");

        assertThat(review.getReviewId()).isEqualTo("rev-1");
        assertThat(review.getProductId()).isEqualTo("prod-1");
        assertThat(review.getUserId()).isEqualTo("user-1");
        assertThat(review.getUserName()).isEqualTo("Alice");
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Great");
        assertThat(review.getVerifiedBuyer()).isTrue();
        assertThat(review.getCreatedAt()).isEqualTo("ts");
    }

    @Test
    void builder_buildsReview() {
        Review review = Review.builder()
                .reviewId("rev-2")
                .productId("prod-2")
                .userId("user-2")
                .userName("Bob")
                .rating(3)
                .comment("Meh")
                .verifiedBuyer(false)
                .createdAt("ts2")
                .build();

        assertThat(review.getReviewId()).isEqualTo("rev-2");
        assertThat(review.getProductId()).isEqualTo("prod-2");
        assertThat(review.getUserId()).isEqualTo("user-2");
        assertThat(review.getUserName()).isEqualTo("Bob");
        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getComment()).isEqualTo("Meh");
        assertThat(review.getVerifiedBuyer()).isFalse();
        assertThat(review.getCreatedAt()).isEqualTo("ts2");
    }

    @Test
    void getReviewId_isAnnotatedAsPartitionKey() throws NoSuchMethodException {
        var method = Review.class.getMethod("getReviewId");

        assertThat(method.getAnnotation(DynamoDbPartitionKey.class)).isNotNull();
    }
}
