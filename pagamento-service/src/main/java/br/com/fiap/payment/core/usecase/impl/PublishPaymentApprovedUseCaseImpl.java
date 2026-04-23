package br.com.fiap.payment.core.usecase.impl;

import br.com.fiap.payment.core.domain.PaymentApprovedEvent;
import br.com.fiap.payment.core.usecase.PublishPaymentApprovedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublishPaymentApprovedUseCaseImpl implements PublishPaymentApprovedUseCase {

    @Override
    public void execute(PaymentApprovedEvent event) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}