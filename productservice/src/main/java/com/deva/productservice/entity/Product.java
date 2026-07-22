package com.deva.productservice.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private String productId;
    private String name;
    private String description;
    private String category;
    private Double price;
    private String imageUrl;

    @DynamoDbPartitionKey
    public String getProductId() { return productId; }

}