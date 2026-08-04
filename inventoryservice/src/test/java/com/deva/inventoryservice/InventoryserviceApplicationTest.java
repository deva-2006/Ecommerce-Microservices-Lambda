package com.deva.inventoryservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class InventoryserviceApplicationTest {

    @Test
    void main_startsSpringApplication() {
        try (MockedStatic<SpringApplication> app = mockStatic(SpringApplication.class)) {
            ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
            app.when(() -> SpringApplication.run(InventoryserviceApplication.class, new String[]{}))
                    .thenReturn(context);

            InventoryserviceApplication.main(new String[]{});

            app.verify(() -> SpringApplication.run(InventoryserviceApplication.class, new String[]{}));
        }
    }
}
