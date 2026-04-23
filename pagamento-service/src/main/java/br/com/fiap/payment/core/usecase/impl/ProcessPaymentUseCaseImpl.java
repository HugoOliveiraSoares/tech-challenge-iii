package br.com.fiap.payment.core.usecase.impl;

import br.com.fiap.payment.core.domain.PedidoCreatedEvent;
import br.com.fiap.payment.core.usecase.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    @Override
    public void execute(PedidoCreatedEvent event) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}