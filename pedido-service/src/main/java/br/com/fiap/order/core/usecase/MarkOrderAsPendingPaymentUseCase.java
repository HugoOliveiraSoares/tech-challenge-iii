package br.com.fiap.order.core.usecase;

import java.util.UUID;

public interface MarkOrderAsPendingPaymentUseCase {
    void execute(UUID orderId);
}
