package br.com.fiap.order.core.exception;

import lombok.Getter;

/** Exceção base com código HTTP e código de erro para a API. */
@Getter
public class SystemBaseException extends RuntimeException {
    private final String code;
    private final Integer status;

    public SystemBaseException(String code, String message, Integer status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
