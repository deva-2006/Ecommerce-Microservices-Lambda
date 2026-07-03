package com.deva.orderservice.client;

import com.deva.orderservice.dto.PaymentRequestDTO;
import com.deva.orderservice.dto.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "payment-service",
        url = "${gateway.url}"
)
public interface PaymentClient {

    @PostMapping("/payments")
    PaymentResponseDTO createPayment(@RequestBody PaymentRequestDTO request);
}