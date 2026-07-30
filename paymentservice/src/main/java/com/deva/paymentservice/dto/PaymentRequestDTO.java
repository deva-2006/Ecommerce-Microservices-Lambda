package com.deva.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentRequestDTO {
    @NotBlank(message = "orderId is required")
    private String orderId;


    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private Double amount;

    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;
}








