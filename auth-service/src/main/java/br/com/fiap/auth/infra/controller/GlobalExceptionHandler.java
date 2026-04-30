package br.com.fiap.auth.infra.controller;

import br.com.fiap.auth.core.exception.SystemBaseException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(SystemBaseException.class)
    protected ResponseEntity<ProblemDetail> handleSystemBaseException(SystemBaseException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(ex.getStatus()), ex.getMessage()
        );
        problemDetail.setType(URI.create("https://api.example.com/errors/" + ex.getCode()));
        problemDetail.setTitle(ex.getCode());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=","")));
        problemDetail.setProperty("code", ex.getCode());
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleGeneralException(final Exception ex, final WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"
        );
        problemDetail.setType(URI.create("https://api.example.com/errors/internal-server-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=","")));
        problemDetail.setProperty("code", "internal.server.error");
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> invalidParams = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> {
                    Map<String, String> param = new HashMap<>();
                    param.put("field", err.getField());
                    param.put("message", err.getDefaultMessage());

                    return param;
                })
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation Failed"
        );
        problemDetail.setType(URI.create("https://api.example.com/errors/validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=","")));
        problemDetail.setProperty("errors", invalidParams);
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }
}
