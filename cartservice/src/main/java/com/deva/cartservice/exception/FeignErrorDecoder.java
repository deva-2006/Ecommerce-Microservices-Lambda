package com.deva.cartservice.exception;

// TODO: FeignErrorDecoder is no longer used - Feign has been replaced with RestClient pattern.
// This class can be removed once all error handling is refactored for RestClient-based calls.
// For now, we comment it out to allow compilation to proceed.
/*
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = "";
        try {
            if (response.body() != null) {
                body = new String(
                        response.body().asInputStream().readAllBytes(),
                        StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            body = "Could not read error response";
        }

        return switch (response.status()) {
            case 404 -> new ResourceNotFoundException(
                    "Downstream service returned 404: " + body);
            case 400 -> new IllegalArgumentException(
                    "Downstream service returned 400: " + body);
            case 409 -> new IllegalStateException(
                    "Downstream service returned 409: " + body);
            case 503 -> new ServiceUnavailableException(
                    "Service unavailable: " + methodKey);
            default  -> new FeignClientException(response.status(),
                    "Feign call failed [" + methodKey + "]: " + body);
        };
    }
}
*/