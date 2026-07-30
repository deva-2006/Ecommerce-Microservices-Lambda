package com.deva.inventoryservice.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
@Getter
@DynamoDbBean
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {


    private String productId;
    private Integer quantity;
    private String updatedAt;

    @DynamoDbPartitionKey
    public String getProductId() { return productId; }


}