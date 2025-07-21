package org.sitmun.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.sitmun.infrastructure.web.dto.DomainExceptionResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class DeserializationExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DomainExceptionResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        List<DomainExceptionResponse.FieldError> errors = new ArrayList<>();
        Throwable cause = ex.getCause();
        
        if (cause instanceof InvalidFormatException ife) {
            for (JsonMappingException.Reference ref : ife.getPath()) {
                errors.add(DomainExceptionResponse.FieldError.builder()
                        .field(ref.getFieldName())
                        .rejectedValue(String.valueOf(ife.getValue()))
                        .message("Invalid value: " + ife.getValue())
                        .build());
            }
        } else if (cause instanceof MismatchedInputException mie) {
            for (JsonMappingException.Reference ref : mie.getPath()) {
                errors.add(DomainExceptionResponse.FieldError.builder()
                        .field(ref.getFieldName())
                        .rejectedValue("")
                        .message("Mismatched input")
                        .build());
            }
        }
        
        String requestPath = null;
        if (request instanceof ServletWebRequest servletRequest) {
            requestPath = servletRequest.getRequest().getRequestURI();
        }
        
        DomainExceptionResponse response = DomainExceptionResponse.builder()
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(ex.getMessage())
                .path(requestPath)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
} 