package com.tangent.wrapper;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link ApiErrorResponse} record and its factory methods.
 */
class ApiErrorResponseTest {

    @Test
    void should_createErrorResponseWithEmptyFieldErrors_when_usingOfFactory() {
        ApiErrorResponse response = ApiErrorResponse.of(404, "Not Found", "Resource missing");

        assertThat(response.success()).isFalse();
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.message()).isEqualTo("Resource missing");
        assertThat(response.fieldErrors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void should_createValidationErrorResponseWithFieldErrors_when_usingValidationFactory() {
        Map<String, String> fields = Map.of("email", "must not be blank");

        ApiErrorResponse response = ApiErrorResponse.validation(400, "Bad Request", "Validation failed", fields);

        assertThat(response.success()).isFalse();
        assertThat(response.fieldErrors()).containsEntry("email", "must not be blank");
    }

    @Test
    void should_allowEmptyFieldErrorsMap_when_validationFactoryCalledWithNoFields() {
        ApiErrorResponse response = ApiErrorResponse.validation(400, "Bad Request", "Validation failed", Map.of());

        assertThat(response.fieldErrors()).isEmpty();
    }
}

