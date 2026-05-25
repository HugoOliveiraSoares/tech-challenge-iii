package br.com.fiap.order.core.exception;

public class UnauthorizedCustomerException extends SystemBaseException {
    public static final String CODE = "order.unauthorized";
    public static final Integer HTTP_STATUS = 403;

    public UnauthorizedCustomerException() {
        super(CODE, "Customer is not allowed to access this order", HTTP_STATUS);
    }
}
