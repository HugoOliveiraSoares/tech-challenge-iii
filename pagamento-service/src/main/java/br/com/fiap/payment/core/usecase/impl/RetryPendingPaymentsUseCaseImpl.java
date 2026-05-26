package br.com.fiap.payment.core.usecase.impl;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentMapperUtil;
import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.core.domain.ProcPagRequest;
import br.com.fiap.payment.core.exception.ExternalServiceUnavailableException;
import br.com.fiap.payment.core.exception.PaymentProcessingException;
import br.com.fiap.payment.core.gateway.PaymentEventGateway;
import br.com.fiap.payment.core.gateway.PaymentGateway;
import br.com.fiap.payment.core.gateway.ProcPagGateway;
import br.com.fiap.payment.core.usecase.RetryPendingPaymentsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RetryPendingPaymentsUseCaseImpl implements RetryPendingPaymentsUseCase {

    private final PaymentGateway paymentGateway;
    private final ProcPagGateway procPagGateway;
    private final PaymentEventGateway eventGateway;

    @Override
    public void execute() {
        var pendingPayments = paymentGateway.findPendingPayments();

        if (pendingPayments.isEmpty()) {
            log.info("Nenhum pagamento pendente para reprocessar");
            return;
        }

        log.info("Encontrados {} pagamentos pendentes para reprocessamento", pendingPayments.size());

        for (var payment : pendingPayments) {
            try {
                processPayment(payment);
            } catch (Exception e) {
                log.error("Erro inesperado ao reprocessar pagamento {}: {}", payment.getPaymentId(), e.getMessage(), e);
            }
        }
    }

    private void processPayment(Payment payment) {
        var currentRetryCount = payment.getRetryCount() != null ? payment.getRetryCount() : 0;

        log.info("Reprocessando pagamento {} (tentativa {})",
                payment.getPaymentId(), currentRetryCount + 1);

        var request = new ProcPagRequest(
                payment.getPaymentId(),
                payment.getClientId(),
                payment.getTotalAmount());

        try {
            var procpagStatus = procPagGateway.processPayment(request);
            PaymentStatus newStatus = PaymentMapperUtil.mapProcpagStatus(procpagStatus);

            payment.changeStatusTo(newStatus);
            payment.incrementRetryCount();
            paymentGateway.save(payment);

            var event = PaymentMapperUtil.buildPaymentEvent(payment);

            if (newStatus == PaymentStatus.APPROVED) {
                eventGateway.publishPaymentApproval(event);
                log.info("Pagamento {} reprocessado com sucesso: APROVADO", payment.getPaymentId());
            } else {
                eventGateway.publishPaymentPending(event);
                log.info("Pagamento {} reprocessado, ainda PENDENTE", payment.getPaymentId());
            }
        } catch (PaymentProcessingException | ExternalServiceUnavailableException e) {
            handleRetryFailure(payment, currentRetryCount, e);
        }
    }

    private void handleRetryFailure(Payment payment, int currentRetryCount, Exception cause) {
        log.warn("Falha no reprocessamento do pagamento {}: {}", payment.getPaymentId(), cause.getMessage());
        payment.incrementRetryCount();
        paymentGateway.save(payment);

        var event = PaymentMapperUtil.buildPaymentEvent(payment);
        try {
            eventGateway.publishPaymentPending(event);
        } catch (Exception e) {
            log.error("Falha ao publicar evento pendente no reprocessamento: {}", e.getMessage(), e);
        }
    }

}
