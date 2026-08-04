package com.deva.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockDeductRequestDtoTest {

    @Test
    void constructorsAndGetters() {
        StockDeductRequestDTO allArgs = new StockDeductRequestDTO(3);
        StockDeductRequestDTO noArgs = new StockDeductRequestDTO();
        noArgs.setQuantity(5);

        assertThat(allArgs.getQuantity()).isEqualTo(3);
        assertThat(noArgs.getQuantity()).isEqualTo(5);
    }

    @Test
    void equals_hashCode_forEqualInstances() {
        StockDeductRequestDTO a = new StockDeductRequestDTO(3);
        StockDeductRequestDTO b = new StockDeductRequestDTO(3);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(new StockDeductRequestDTO(4));
    }
}
