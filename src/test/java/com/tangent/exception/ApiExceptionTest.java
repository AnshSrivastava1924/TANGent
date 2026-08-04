package com.tangent.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiException}.
 */
class ApiExceptionTest {

    @Test
    void should_storeStatusAndMessage_when_constructed() {
        ApiException exception = new ApiException(HttpStatus.BAD_REQUEST, "Invalid input");

        assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).isEqualTo("Invalid input");
    }

    @Test
    void should_beRuntimeException_when_thrown() {
        ApiException exception = new ApiException(HttpStatus.NOT_FOUND, "not found");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_allowNullMessage_when_constructedWithNullMessage() {
        ApiException exception = new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, null);

        assertThat(exception.getMessage()).isNull();
        assertThat(exception.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

