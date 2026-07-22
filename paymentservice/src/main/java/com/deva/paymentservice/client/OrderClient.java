package com.deva.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "order-service",
        url = "${gateway.url}"
)
public interface OrderClient {

    @GetMapping("/orders/{orderId}")
    java.util.Map<String, Object> getOrderById(@PathVariable String orderId);

    @PutMapping("/orders/{orderId}/status")
    void updateOrderStatus(@PathVariable String orderId, @RequestParam String status);
}