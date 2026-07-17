package com.theisraelayooluwa.absencebackend.controllers;

import com.theisraelayooluwa.absencebackend.dto.ApiResponse;
import com.theisraelayooluwa.absencebackend.exception.ForbiddenOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.Arrays;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessRuleViolation(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(HttpStatus.CONFLICT.value(), ex.getMessage(), null));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbidden(ForbiddenOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(HttpStatus.FORBIDDEN.value(), ex.getMessage(), null));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), null));
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiResponse<Object>> handleAuthFailure(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatError)
                .toList();

        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), String.join("; ", errors), null));
    }

    private String formatError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            Class<?> target = ife.getTargetType();
            if (target != null && target.isEnum()) {
                String field = "value";
                if (!ife.getPath().isEmpty() && ife.getPath().get(0).getFieldName() != null) {
                    field = ife.getPath().get(0).getFieldName();
                }
                String invalid = String.valueOf(ife.getValue());
                String allowed = Arrays.stream(target.getEnumConstants())
                        .map(Object::toString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                String msg = String.format("%s: invalid value '%s'. Allowed values: [%s]", field, invalid, allowed);
                return ResponseEntity.badRequest().body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), msg, null));
            }
        }

        return ResponseEntity.badRequest().body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null));
    }
}
