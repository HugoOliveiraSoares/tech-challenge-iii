package br.com.fiap.order.core.usecase;

import java.util.UUID;

public interface MarkOrderAsPaidUseCase {
    void execute(UUID orderId);
}
