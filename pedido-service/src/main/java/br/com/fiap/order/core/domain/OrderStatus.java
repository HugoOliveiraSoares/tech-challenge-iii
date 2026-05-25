package br.com.fiap.order.core.domain;

/** Ciclo de vida do pedido (mapeado para strings na API REST). */
public enum OrderStatus {
    CREATED,           // aguardando processamento do pagamento
    PENDING_PAYMENT,   // pagamento-service indisponível ou em retry
    PAID               // pagamento confirmado
}