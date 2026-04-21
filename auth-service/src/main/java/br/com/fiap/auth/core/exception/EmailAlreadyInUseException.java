package br.com.fiap.auth.core.exception;

public class EmailAlreadyInUseException extends SystemBaseException {
    private static final String CODE = "user.actionNotAllowed";
    private static final Integer HTTP_STATUS = 409;

    public EmailAlreadyInUseException(String message) {
        super(CODE, message, HTTP_STATUS);
    }
}
