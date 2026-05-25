package br.com.fiap.order.core.usecase;

import java.util.UUID;

/** Caso de uso: marcar pedido como PAGO (consumo de pagamento-aprovado). */
public interface MarkOrderAsPaidUseCase {
    void execute(UUID orderId);
}
