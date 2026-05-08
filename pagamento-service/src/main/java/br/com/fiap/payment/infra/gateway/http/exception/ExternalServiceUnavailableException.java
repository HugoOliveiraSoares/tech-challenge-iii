package br.com.fiap.payment.infra.gateway.http.exception;

import br.com.fiap.payment.core.exception.SystemBaseException;

public class ExternalServiceUnavailableException extends SystemBaseException {

    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
