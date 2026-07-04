package com.deva.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "order-service",
        url = "${gateway.url}"
)
public interface OrderClient {

    @PutMapping("/orders/{orderId}/status")
    void updateOrderStatus(@PathVariable String orderId, @RequestParam String status);

    @PostMapping("/orders/{orderId}/payment-success")
    void handlePaymentSuccess(@PathVariable String orderId);
}