package com.deva.cartservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.deva.cartservice.client")
public class FeignClientConfig {
}
