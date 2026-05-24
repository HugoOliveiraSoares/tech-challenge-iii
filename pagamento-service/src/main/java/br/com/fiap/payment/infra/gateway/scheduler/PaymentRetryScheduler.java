package br.com.fiap.payment.infra.gateway.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.fiap.payment.core.usecase.RetryPendingPaymentsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private final RetryPendingPaymentsUseCase retryPendingPaymentsUseCase;

    @Scheduled(fixedDelayString = "${payment.retry.scheduled-interval:60000}")
    public void retryPendingPayments() {
        log.info("Iniciando reprocessamento agendado de pagamentos pendentes");
        retryPendingPaymentsUseCase.execute();
    }
}
