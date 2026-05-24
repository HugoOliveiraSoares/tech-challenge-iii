package br.com.fiap.payment.core.usecase.impl;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.fiap.payment.core.domain.OrderEvent;
import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentMapperUtil;
import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.core.domain.ProcPagRequest;
import br.com.fiap.payment.core.exception.ExternalServiceUnavailableException;
import br.com.fiap.payment.core.exception.OrderAlreadyCreatedException;
import br.com.fiap.payment.core.exception.PaymentProcessingException;
import br.com.fiap.payment.core.gateway.PaymentEventGateway;
import br.com.fiap.payment.core.gateway.PaymentGateway;
import br.com.fiap.payment.core.gateway.ProcPagGateway;
import br.com.fiap.payment.core.usecase.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementação do caso de uso para processamento de pagamentos.
 * Orquestra validação de pedido, chamada ao Procpag, atualização de status e
 * publicação de eventos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {
    private final PaymentGateway paymentGateway;
    private final ProcPagGateway procPagGateway;
    private final PaymentEventGateway eventGateway;

    /**
     * Processa pagamento de um pedido com base no evento recebido.
     * 
     * @param event Evento de criação de pedido com dados para pagamento
     * @throws OrderAlreadyCreatedException se já existir pagamento para o pedido
     * @throws IllegalArgumentException     se dados do evento forem inválidos
     */
    @Override
    public void execute(OrderEvent event) {

        validateEvent(event);

        var payment = resolvePayment(event);
        if (payment == null) {
            return;
        }

        log.info("Processando pagamento de pedido {}", event.orderId());

        try {

            var procPagRequest = new ProcPagRequest(
                    payment.getPaymentId(),
                    payment.getClientId(),
                    payment.getTotalAmount());

            var procpagStatus = procPagGateway.processPayment(procPagRequest);

            log.info("Status Procpag para pedido {}: {}", event.orderId(), procpagStatus);

            PaymentStatus newStatus = PaymentMapperUtil.mapProcpagStatus(procpagStatus);

            payment.changeStatusTo(newStatus);

            paymentGateway.save(payment);

            var paymentEvent = PaymentMapperUtil.buildPaymentEvent(payment);

            if (newStatus.equals(PaymentStatus.APPROVED)) {
                eventGateway.publishPaymentApproval(paymentEvent);
            } else {
                eventGateway.publishPaymentPending(paymentEvent);
            }

        } catch (PaymentProcessingException | ExternalServiceUnavailableException e) {
            log.error("Erro no processamento do pedido {}", event.orderId(), e);
            handleFailure(payment, e);
        }
    }

    private Payment resolvePayment(OrderEvent event) {
        Optional<Payment> existing = paymentGateway.findPaymentByOrderId(event.orderId());
        if (existing.isEmpty()) {
            return Payment.createPending(
                    event.orderId(),
                    event.clientId(),
                    event.totalAmount());
        }
        Payment payment = existing.get();
        if (payment.getPaymentStatus() == PaymentStatus.APPROVED) {
            log.warn("Pedido {} já foi aprovado. Ignorando evento duplicado.", event.orderId());
            return null;
        }
        log.info("Re-processando pagamento pendente do pedido {}", event.orderId());
        return payment;
    }

    private void validateEvent(OrderEvent event) {
        if (event.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do pedido deve ser positivo");
        }
        if (event.clientId() == null || event.clientId().isBlank()) {
            throw new IllegalArgumentException("ID do cliente não pode ser vazio");
        }
        if (event.orderId() == null || event.orderId().isBlank()) {
            throw new IllegalArgumentException("ID do pedido não pode ser vazio");
        }

    }

    private void handleFailure(Payment payment, Exception cause) {
        if (payment.getPaymentStatus() == PaymentStatus.APPROVED) {
            log.warn("Pagamento {} já aprovado, ignorando tentativa de reverter para PENDING",
                    payment.getPaymentId());
            return;
        }
        payment.changeStatusTo(PaymentStatus.PENDING);
        paymentGateway.save(payment);

        var paymentEvent = PaymentMapperUtil.buildPaymentEvent(payment);
        try {
            eventGateway.publishPaymentPending(paymentEvent);
        } catch (Exception e) {
            log.error("Falha ao publicar evento pendente para pedido {}: {}", payment.getOrderId(), e.getMessage(), e);
        }
    }
}
