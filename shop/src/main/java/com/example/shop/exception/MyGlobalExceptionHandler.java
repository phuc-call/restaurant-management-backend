package com.example.shop.exception;

import com.example.shop.payloads.reponse.APIResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice

public class MyGlobalExceptionHandler extends RuntimeException{
    // Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> handleResourceNotFound(ResourceNotFoundException e) {
        return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.NOT_FOUND);
    }

    // Custom API logic error
    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> handleAPIException(APIException e) {
        return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    // Validation error in RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException e) {
        Map<String, String> res = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String field = ((FieldError) err).getField();
            String msg = err.getDefaultMessage();
            res.put(field, msg);
        });
        return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
    }

    // Validation error in Path / Params
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> res = new HashMap<>();
        e.getConstraintViolations().forEach(v ->
                res.put(v.getPropertyPath().toString(), v.getMessage())
        );
        return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
    }

    // Authentication error
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthException(AuthenticationException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // Missing path variable
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<APIResponse> handleMissingPathVar(MissingPathVariableException e) {
        return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    // DB constraint violation
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        return new ResponseEntity<>(new APIResponse("Duplicate or invalid data", false), HttpStatus.CONFLICT);
    }

}
