package br.com.fiap.payment.core.usecase.impl;

import br.com.fiap.payment.core.domain.PaymentPendingEvent;
import br.com.fiap.payment.core.usecase.PublishPaymentPendingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublishPaymentPendingUseCaseImpl implements PublishPaymentPendingUseCase {

    @Override
    public void execute(PaymentPendingEvent event) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}