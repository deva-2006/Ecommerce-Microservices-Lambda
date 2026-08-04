package com.deva.cartservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class CartserviceApplicationTest {

    @Test
    void main_runsSpringApplication() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            CartserviceApplication.main(new String[]{"--server.port=0"});

            mocked.verify(() -> SpringApplication.run(eq(CartserviceApplication.class), any(String[].class)));
        }
    }
}
