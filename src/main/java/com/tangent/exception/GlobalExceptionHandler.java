package com.tangent.exception;

import com.tangent.wrapper.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return error(exception.status(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ApiErrorResponse body = ApiErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Request validation failed", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleMissingResource(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message));
    }
}
