package br.com.fiap.payment.core.exception;

import lombok.Getter;

@Getter
public class SystemBaseException extends RuntimeException {

    public SystemBaseException(String message) {
        super(message);
    }

}
