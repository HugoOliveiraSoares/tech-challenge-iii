package br.com.fiap.payment.core.usecase.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.fiap.payment.core.domain.OrderEvent;
import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentEvent;
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

        log.info("Processando pagamento de pedido {}", event.orderId());

        Optional<Payment> paymentByOrderIdAndApproved = paymentGateway
                .findPaymentByOrderIdAndApproved(event.orderId());

        if (paymentByOrderIdAndApproved.isPresent()) {
            log.warn("Pedido com id {} já foi criado", event.orderId());
            return;
        }

        var payment = new Payment(
                event.orderId(),
                event.clientId(),
                event.totalAmount(),
                PaymentStatus.PENDING);

        var paymentSaved = paymentGateway.save(payment);

        try {

            var procPagRequest = new ProcPagRequest(
                    paymentSaved.getPaymentId(),
                    paymentSaved.getClientId(),
                    paymentSaved.getTotalAmount());

            var procpagStatus = procPagGateway.processarPagamento(procPagRequest);

            log.info("Status Procpag para pedido {}: {}", event.orderId(), procpagStatus);

            PaymentStatus newStatus = mapProcpagStatus(procpagStatus);

            paymentSaved.changeStatusTo(newStatus);

            paymentGateway.save(paymentSaved);

            var paymentEvent = buildPaymentEvent(paymentSaved);

            if (newStatus.equals(PaymentStatus.APPROVED)) {
                eventGateway.publishPaymentApproval(paymentEvent);
            } else {
                eventGateway.publishPaymentPending(paymentEvent);
            }

        } catch (PaymentProcessingException | ExternalServiceUnavailableException e) {
            log.error("Erro no processamento do pedido {}", event.orderId(), e);
            handleFailure(paymentSaved, e);
        } catch (Exception e) {
            log.error("Erro inesperado no pedido {}", event.orderId(), e);
            throw e;
        }
    }

    private void validateEvent(OrderEvent event) {
        if (event.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do pedido deve ser positivo");
        }
        if (event.clientId() == null || event.clientId().isBlank()) {
            throw new IllegalArgumentException("ID do cliente não pode ser vazio");
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

    private void handleFailure(Payment payment, Exception cause) {
        if (payment.getPaymentStatus() == PaymentStatus.APPROVED) {
            log.warn("Pagamento {} já aprovado, ignorando tentativa de reverter para PENDING",
                    payment.getPaymentId());
            return;
        }
        payment.changeStatusTo(PaymentStatus.PENDING);
        paymentGateway.save(payment);

        var paymentEvent = buildPaymentEvent(payment);
        try {
            eventGateway.publishPaymentPending(paymentEvent);
        } catch (Exception e) {
            log.error("Falha ao publicar evento pendente para pedido {}: {}", payment.getOrderId(), e.getMessage(), e);
        }
    }
}
