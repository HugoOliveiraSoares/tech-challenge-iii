package br.com.fiap.auth.core.exception;

public class DomainException extends SystemBaseException {
    private static final String CODE = "user.invalid_data";
    private static final Integer HTTP_STATUS = 400;

    public DomainException(String message) {
        super(CODE, message, HTTP_STATUS);
    }
}
