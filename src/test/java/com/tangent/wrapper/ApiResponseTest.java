package com.tangent.wrapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link ApiResponse} record and its factory methods.
 */
class ApiResponseTest {

    @Test
    void should_createSuccessResponseWithNullMessage_when_usingSingleArgFactory() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isNull();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void should_createSuccessResponseWithMessage_when_usingTwoArgFactory() {
        ApiResponse<String> response = ApiResponse.success("Created", "payload");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Created");
        assertThat(response.data()).isEqualTo("payload");
    }

    @Test
    void should_allowNullData_when_dataIsNull() {
        ApiResponse<Object> response = ApiResponse.success(null);

        assertThat(response.data()).isNull();
        assertThat(response.success()).isTrue();
    }
}

