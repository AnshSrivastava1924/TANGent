package com.tangent.exception;

import com.tangent.wrapper.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void should_returnMappedStatusAndMessage_when_handlingApiException() {
        ApiException exception = new ApiException(HttpStatus.CONFLICT, "Duplicate entry");

        ResponseEntity<ApiErrorResponse> response = handler.handleApiException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Duplicate entry");
    }

    @Test
    void should_returnBadRequestWithFieldErrors_when_handlingValidationException() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", "email", "must not be blank");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fieldErrors()).containsEntry("email", "must not be blank");
        assertThat(response.getBody().message()).isEqualTo("Request validation failed");
    }

    @Test
    void should_keepFirstErrorPerField_when_multipleErrorsExistForSameField() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError first = new FieldError("request", "email", "must not be blank");
        FieldError second = new FieldError("request", "email", "must be a valid email");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(first, second));

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception);

        assertThat(response.getBody().fieldErrors()).containsEntry("email", "must not be blank");
        assertThat(response.getBody().fieldErrors()).hasSize(1);
    }

    @Test
    void should_returnNotFound_when_handlingMissingResource() {
        NoResourceFoundException exception = mock(NoResourceFoundException.class);

        ResponseEntity<ApiErrorResponse> response = handler.handleMissingResource(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Resource not found");
    }

    @Test
    void should_returnInternalServerError_when_handlingUnexpectedException() {
        Exception exception = new IllegalStateException("boom");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error");
    }

    @Test
    void should_notLeakInternalExceptionMessage_when_handlingUnexpectedException() {
        Exception exception = new RuntimeException("sensitive internal detail: password=secret");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(exception);

        assertThat(response.getBody().message()).doesNotContain("sensitive internal detail");
    }
}

