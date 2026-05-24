package br.com.fiap.payment.core.exception;

public class ExternalServiceUnavailableException extends SystemBaseException {

    public ExternalServiceUnavailableException(String message) {
        super(message);
    }

    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
