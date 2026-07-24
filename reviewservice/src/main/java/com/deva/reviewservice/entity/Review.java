package com.deva.reviewservice.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    private String reviewId;
    private String productId;
    private String userId;
    private String userName;
    private Integer rating;
    private String comment;
    private Boolean verifiedBuyer;
    private String createdAt;

    @DynamoDbPartitionKey
    public String getReviewId() {
        return reviewId;
    }
}
