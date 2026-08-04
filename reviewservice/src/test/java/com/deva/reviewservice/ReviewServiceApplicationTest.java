package com.deva.reviewservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class ReviewServiceApplicationTest {

    @Test
    void isAnnotatedAsSpringBootApplication() {
        assertThat(ReviewServiceApplication.class.getAnnotation(SpringBootApplication.class)).isNotNull();
        assertThat(ReviewServiceApplication.class.getAnnotation(EnableFeignClients.class)).isNotNull();
    }

    @Test
    void main_startsSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            ReviewServiceApplication.main(new String[]{"--test"});

            spring.verify(() -> SpringApplication.run(ReviewServiceApplication.class, new String[]{"--test"}));
        }
    }
}
