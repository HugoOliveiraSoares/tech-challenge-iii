package br.com.fiap.payment.core.exception;

public class PaymentProcessingException extends SystemBaseException {

    public PaymentProcessingException(String message) {
        super(message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

}
