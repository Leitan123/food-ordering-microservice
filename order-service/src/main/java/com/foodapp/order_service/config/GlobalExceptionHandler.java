package com.foodapp.order_service.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage();
        
        if (message != null && (message.contains("not found") || message.contains("unavailable") || message.contains("communicating"))) {
            error.put("error", "Bad Request");
            error.put("message", message);
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
        
        error.put("error", "Internal Server Error");
        error.put("message", message);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
