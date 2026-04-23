package br.com.fiap.payment.core.usecase.impl;

import br.com.fiap.payment.core.usecase.ConfirmPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmPaymentUseCaseImpl implements ConfirmPaymentUseCase {

    @Override
    public void execute(String paymentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}