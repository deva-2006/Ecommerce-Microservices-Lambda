package com.deva.orderservice.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@DynamoDbBean
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {


    private String orderId;
    private String paymentId;
    private String userId;
    private List<OrderItem> items;
    private Double totalAmount;
    private String status;
    private String shippingAddress;
    private String createdAt;
    private String fulfillmentStatus;

    @DynamoDbPartitionKey
    public String getOrderId() { return orderId; }
}