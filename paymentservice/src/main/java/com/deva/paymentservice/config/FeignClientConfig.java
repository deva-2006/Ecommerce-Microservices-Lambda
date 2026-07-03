package com.deva.paymentservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.deva.paymentservice.client")
public class FeignClientConfig {
}
