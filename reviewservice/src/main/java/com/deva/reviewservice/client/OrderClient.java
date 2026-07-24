package com.deva.reviewservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "order-service", url = "${orderservice.url:https://73svzbgcrf.execute-api.us-east-1.amazonaws.com}")
public interface OrderClient {

    @GetMapping("/orders/verify-purchase")
    Map<String, Boolean> verifyPurchase(
            @RequestHeader("Authorization") String token,
            @RequestParam("productId") String productId);
}
