package com.beloveddesigns.except;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ApiError> handleProductUnavailable(ProductUnavailableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", ex.getMessage(), request, null);
    }

    @ExceptionHandler(OrderNotPendingException.class)
    public ResponseEntity<ApiError> handleOrderNotPending(OrderNotPendingException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "ORDER_NOT_PENDING", ex.getMessage(), request, null);
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ApiError> handleEmptyCart(EmptyCartException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "EMPTY_CART", ex.getMessage(), request, null);
    }

    /** Route d'API inconnue : 404 (avant : attrapée par le gestionnaire générique → 500). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoRoute(NoResourceFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ErrorMessages.ROUTE_NOT_FOUND, request, null);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiError> handleUnauthenticated(AuthenticationCredentialsNotFoundException ex,
                                                          HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ErrorMessages.AUTHENTICATION_REQUIRED, request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ErrorMessages.INVALID_CREDENTIALS, request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ErrorMessages.VALIDATION_ERROR, request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", ErrorMessages.MALFORMED_REQUEST, request, null);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiError> handleExpiredJwt(ExpiredJwtException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", ErrorMessages.TOKEN_EXPIRED, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Sans ce log, toute erreur 500 est invisible (le client ne reçoit qu'un message générique).
        log.error("Erreur inattendue sur {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ErrorMessages.INTERNAL_ERROR, request, null);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String code, String message,
                                                     HttpServletRequest request, List<String> details) {
        ApiError error = new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(), details);
        return ResponseEntity.status(status).body(error);
    }
}
