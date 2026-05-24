package br.com.fiap.payment.core.usecase.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentEvent;
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
@Service
@RequiredArgsConstructor
public class RetryPendingPaymentsUseCaseImpl implements RetryPendingPaymentsUseCase {

    private final PaymentGateway paymentGateway;
    private final ProcPagGateway procPagGateway;
    private final PaymentEventGateway eventGateway;

    @Override
    public void execute() {
        var pendingPayments = paymentGateway.findPendingWithRetryCountLessThan3();

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
            var procpagStatus = procPagGateway.processarPagamento(request);
            PaymentStatus newStatus = mapProcpagStatus(procpagStatus);

            payment.changeStatusTo(newStatus);
            payment.incrementRetryCount();
            paymentGateway.save(payment);

            var event = buildPaymentEvent(payment);

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

        var event = buildPaymentEvent(payment);
        try {
            eventGateway.publishPaymentPending(event);
        } catch (Exception e) {
            log.error("Falha ao publicar evento pendente no reprocessamento: {}", e.getMessage(), e);
        }
    }

    private PaymentStatus mapProcpagStatus(String procpagStatus) {
        return switch (procpagStatus.toUpperCase()) {
            case "ACCEPTED" -> PaymentStatus.APPROVED;
            case "PENDING" -> PaymentStatus.PENDING;
            default -> {
                log.warn("Status desconhecido Procpag: {}, assumindo REJECTED", procpagStatus);
                yield PaymentStatus.PENDING;
            }
        };
    }

    private PaymentEvent buildPaymentEvent(Payment payment) {
        return new PaymentEvent(
                payment.getOrderId(),
                payment.getPaymentId().toString(),
                payment.getTotalAmount(),
                LocalDateTime.now());
    }
}
