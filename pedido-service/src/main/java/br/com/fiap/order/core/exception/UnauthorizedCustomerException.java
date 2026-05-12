package br.com.fiap.order.core.exception;

public class UnauthorizedCustomerException extends RuntimeException {
    public UnauthorizedCustomerException(String message) {
        super(message);
    }
}
