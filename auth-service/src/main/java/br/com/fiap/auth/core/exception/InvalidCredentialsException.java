package br.com.fiap.auth.core.exception;

public class InvalidCredentialsException extends SystemBaseException {
    public static final String CODE = "auth.invalidCredentials";
    public static final String MESSAGE = "Invalid username or password";
    public static final Integer HTTP_STATUS = 401;

    public InvalidCredentialsException() {
        super(CODE, MESSAGE, HTTP_STATUS);
    }
}
