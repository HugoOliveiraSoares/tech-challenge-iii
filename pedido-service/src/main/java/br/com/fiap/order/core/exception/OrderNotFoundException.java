package br.com.fiap.order.core.exception;

/** Pedido não encontrado no banco (HTTP 404). */
public class OrderNotFoundException extends SystemBaseException {
    public static final String CODE = "order.not_found";
    public static final Integer HTTP_STATUS = 404;

    public OrderNotFoundException() {
        super(CODE, "Order not found", HTTP_STATUS);
    }
}
