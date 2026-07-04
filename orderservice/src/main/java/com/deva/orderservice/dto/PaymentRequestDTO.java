package com.deva.orderservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private String orderId;
    private Double amount;
    private String paymentMethod;
}