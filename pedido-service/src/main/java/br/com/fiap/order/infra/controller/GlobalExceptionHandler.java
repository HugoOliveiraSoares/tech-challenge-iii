package br.com.fiap.order.infra.controller;

import br.com.fiap.order.core.exception.SystemBaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

/** Converte exceções de domínio e validação em respostas HTTP (ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_ERROR_ID = "validation-error";
    private static final String INTERNAL_SERVER_ERROR_ID = "internal-server-error";

    @Value("${api.problems.base-uri:http://localhost:8082/problems}")
    private String problemsBaseUri;

    @ExceptionHandler(SystemBaseException.class)
    protected ResponseEntity<ProblemDetail> handleSystemBaseException(SystemBaseException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(ex.getStatus()), ex.getMessage()
        );
        problemDetail.setType(problemTypeUri(ex.getCode()));
        problemDetail.setTitle(ex.getCode());
        problemDetail.setInstance(requestInstanceUri(request));
        problemDetail.setProperty("code", ex.getCode());
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleGeneralException(final Exception ex, final WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"
        );
        problemDetail.setType(problemTypeUri(INTERNAL_SERVER_ERROR_ID));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(requestInstanceUri(request));
        problemDetail.setProperty("code", "internal.server.error");
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  org.springframework.http.HttpHeaders headers,
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
        problemDetail.setType(problemTypeUri(VALIDATION_ERROR_ID));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(requestInstanceUri(request));
        problemDetail.setProperty("errors", invalidParams);
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    /** URI do tipo de problema (RFC 7807 `type`), configurável via api.problems.base-uri. */
    private URI problemTypeUri(String problemId) {
        String base = problemsBaseUri.endsWith("/")
                ? problemsBaseUri.substring(0, problemsBaseUri.length() - 1)
                : problemsBaseUri;
        return URI.create(base + "/" + problemId);
    }

    /** URI da requisição que gerou o erro (RFC 7807 `instance`). */
    private URI requestInstanceUri(WebRequest request) {
        return URI.create(request.getDescription(false).replace("uri=", ""));
    }
}
