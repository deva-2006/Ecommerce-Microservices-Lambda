package com.deva.paymentservice.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequest_hasNoViolations() {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setOrderId("order-1");
        dto.setAmount(99.99);
        dto.setPaymentMethod("CREDIT_CARD");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void missingOrderId_isRejected() {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setAmount(99.99);
        dto.setPaymentMethod("CREDIT_CARD");

        assertThat(validator.validate(dto))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("orderId");
    }

    @Test
    void missingAmount_isRejected() {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setOrderId("order-1");
        dto.setPaymentMethod("CREDIT_CARD");

        assertThat(validator.validate(dto))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void nonPositiveAmount_isRejected() {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setOrderId("order-1");
        dto.setAmount(-5.0);
        dto.setPaymentMethod("CREDIT_CARD");

        assertThat(validator.validate(dto))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void missingPaymentMethod_isRejected() {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setOrderId("order-1");
        dto.setAmount(99.99);

        assertThat(validator.validate(dto))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("paymentMethod");
    }
}
