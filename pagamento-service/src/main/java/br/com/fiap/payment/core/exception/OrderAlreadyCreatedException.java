package br.com.fiap.payment.core.exception;

public class OrderAlreadyCreatedException extends SystemBaseException {

    public OrderAlreadyCreatedException(String message) {
        super(message);
    }

}
