package com.shopflow.common.api;

import com.shopflow.common.web.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Menerjemahkan exception menjadi {@link ApiError}.
 *
 * <p>Ini di-registrasikan sebagai bean oleh {@code PlatformCommonAutoConfiguration},
 * sehingga SETIAP service yang memakai platform-common otomatis punya error handling
 * yang sama tanpa harus menulis ulang.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        // Error bisnis: log ringkas, tanpa stack trace (bukan bug).
        log.warn("Business error [{}] {} -> {}", ex.status().value(), request.getRequestURI(), ex.getMessage());
        return build(ex.status(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Data yang dikirim tidak valid", request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldViolation(String.valueOf(v.getPropertyPath()), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Data yang dikirim tidak valid", request, violations);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleUnreadable(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Body atau parameter request tidak dapat dibaca", request, List.of());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNoHandler(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Endpoint tidak ditemukan", request, List.of());
    }

    /**
     * Jaring terakhir. Pesan asli TIDAK dikirim ke klien supaya detail internal
     * (nama tabel, stack trace, dsb.) tidak bocor. Yang bocor hanya traceId untuk korelasi log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception di {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan pada server", request, List.of());
    }

    private ApiError.FieldViolation toViolation(FieldError error) {
        return new ApiError.FieldViolation(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status,
                                           String message,
                                           HttpServletRequest request,
                                           List<ApiError.FieldViolation> violations) {
        ApiError body = ApiError.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Correlation.currentTraceId(),
                violations);
        return ResponseEntity.status(status).body(body);
    }
}
