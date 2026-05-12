package br.com.fiap.order.core.usecase;

import java.util.UUID;

/** Caso de uso: marcar pedido como pendente (consumo de pagamento-pendente). */
public interface MarkOrderAsPendingPaymentUseCase {
    void execute(UUID orderId);
}
