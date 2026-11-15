package com.saas.platform.catalog.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDup(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }
    @ExceptionHandler({BusinessException.class, InsufficientStockException.class, InvalidOperationException.class})
    public ResponseEntity<Map<String, Object>> handleBusiness(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccess(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta accion");
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValid(MethodArgumentNotValidException ex) {
        Map<String, Object> resp = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> errors.put(((FieldError) e).getField(), e.getDefaultMessage()));
        resp.put("timestamp", LocalDateTime.now());
        resp.put("status", HttpStatus.BAD_REQUEST.value());
        resp.put("message", "Errores de validacion");
        resp.put("errors", errors);
        return ResponseEntity.badRequest().body(resp);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Error inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + ex.getMessage());
    }
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("timestamp", LocalDateTime.now());
        r.put("status", status.value());
        r.put("message", msg);
        return ResponseEntity.status(status).body(r);
    }
}
