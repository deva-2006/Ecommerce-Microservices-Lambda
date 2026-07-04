package com.deva.cartservice.config;

import com.deva.cartservice.security.AuthUserIdArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;



@Configuration
public class WebConfig implements WebMvcConfigurer {

    //tells spring there is a custom argument resolver to be used for controller methods
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthUserIdArgumentResolver()); // Whenever you see @AuthUserId, call this class
    }
}