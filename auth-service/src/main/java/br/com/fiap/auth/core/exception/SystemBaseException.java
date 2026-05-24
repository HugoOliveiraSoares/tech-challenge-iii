package br.com.fiap.auth.core.exception;

import lombok.Getter;

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
