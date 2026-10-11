package com.inventory.stockalert.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.inventory.stockalert.dto.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(
            ApiException exception,
            WebRequest request) {

        return respond(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                Map.of(),
                new HttpHeaders(),
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error -> {
            String message = error.getDefaultMessage() == null
                    ? "Invalid value."
                    : error.getDefaultMessage();

            fieldErrors
                    .computeIfAbsent(
                            error.getField(),
                            field -> new ArrayList<>()
                    )
                    .add(message);
        });

        return respond(
                status,
                "VALIDATION_ERROR",
                "One or more fields are invalid.",
                fieldErrors,
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        return respond(
                status,
                "INVALID_REQUEST_BODY",
                "Request body is missing or contains invalid JSON or field values.",
                Map.of(),
                headers,
                request
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleConcurrentUpdate(
            OptimisticLockingFailureException exception,
            WebRequest request) {

        return respond(
                HttpStatus.CONFLICT,
                "CONCURRENT_UPDATE",
                "The product changed during this operation. Reload it and try again.",
                Map.of(),
                new HttpHeaders(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(
            DataIntegrityViolationException exception,
            WebRequest request) {

        // Also handles duplicate SKUs caused by simultaneous requests.
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof
                    org.hibernate.exception.ConstraintViolationException violation
                    && "uk_products_sku".equals(
                            violation.getConstraintName())) {

                return respond(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_SKU",
                        "A product already exists with this SKU.",
                        Map.of(),
                        new HttpHeaders(),
                        request
                );
            }

            cause = cause.getCause();
        }

        log.warn("A database integrity constraint rejected an operation.",
                exception);

        return respond(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "The operation conflicts with existing data or database constraints.",
                Map.of(),
                new HttpHeaders(),
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        HttpStatus knownStatus = HttpStatus.resolve(status.value());

        String code = knownStatus == null
                ? "HTTP_ERROR"
                : knownStatus.name();

        String message = knownStatus == null
                ? "The request could not be processed."
                : knownStatus.getReasonPhrase();

        if (status.is5xxServerError()) {
            log.error("An HTTP request failed.", exception);
            message = "An unexpected server error occurred.";
        }

        ApiError error = createError(
                status,
                code,
                message,
                Map.of(),
                request
        );

        return super.handleExceptionInternal(
                exception,
                error,
                headers,
                status,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(
            Exception exception,
            WebRequest request) {

        log.error("An unexpected application error occurred.", exception);

        return respond(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred.",
                Map.of(),
                new HttpHeaders(),
                request
        );
    }

    private ResponseEntity<Object> respond(
            HttpStatusCode status,
            String code,
            String message,
            Map<String, List<String>> fieldErrors,
            HttpHeaders headers,
            WebRequest request) {

        return new ResponseEntity<>(
                createError(status, code, message, fieldErrors, request),
                headers,
                status
        );
    }

    private ApiError createError(
            HttpStatusCode status,
            String code,
            String message,
            Map<String, List<String>> fieldErrors,
            WebRequest request) {

        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "";

        return new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                code,
                message,
                path,
                fieldErrors
        );
    }
}