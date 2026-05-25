package br.com.fiap.order.core.exception;

public class InvalidOrderException extends SystemBaseException {
    public static final String CODE = "order.invalid";
    public static final Integer HTTP_STATUS = 400;

    public InvalidOrderException(String message) {
        super(CODE, message, HTTP_STATUS);
    }
}
