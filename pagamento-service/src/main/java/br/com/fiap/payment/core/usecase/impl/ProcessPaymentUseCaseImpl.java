package br.com.fiap.payment.core.usecase.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import br.com.fiap.payment.core.domain.OrderEvent;
import br.com.fiap.payment.core.domain.Payment;
import br.com.fiap.payment.core.domain.PaymentEvent;
import br.com.fiap.payment.core.domain.PaymentStatus;
import br.com.fiap.payment.core.domain.ProcPagRequest;
import br.com.fiap.payment.core.exception.OrderAlreadyCreatedException;
import br.com.fiap.payment.core.gateway.PaymentEventGateway;
import br.com.fiap.payment.core.gateway.PaymentGateway;
import br.com.fiap.payment.core.gateway.ProcPagGateway;
import br.com.fiap.payment.core.usecase.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    private final PaymentGateway paymentGateway;
    private final ProcPagGateway procPagGateway;
    private final PaymentEventGateway eventGateway;

    @Override
    public void execute(OrderEvent event) {

        log.info("Processando pagamento de pedido {} criado.", event.orderId());

        paymentGateway
                .findPaymentByOrderId(event.orderId())
                .ifPresent(existing -> {
                    throw new OrderAlreadyCreatedException(
                            "Pedido com id {} já foi criado".formatted(event.orderId()));
                });

        var payment = new Payment(
                event.orderId(),
                event.clientId(),
                event.totalAmount(),
                PaymentStatus.PENDING);

        var paymentSaved = paymentGateway.save(payment);

        try {
            var request = new ProcPagRequest(
                    paymentSaved.getPaymentId(),
                    paymentSaved.getOrderId(),
                    paymentSaved.getTotalAmount());

            procPagGateway.requisicao(request);

            paymentSaved.changeStatusTo(PaymentStatus.APROVED);
            paymentGateway.save(paymentSaved);

            var paymentEvent = new PaymentEvent(
                    paymentSaved.getOrderId(),
                    paymentSaved.getPaymentId().toString(),
                    paymentSaved.getTotalAmount(),
                    LocalDateTime.now());

            eventGateway.publishPaymentApproval(paymentEvent);

        } catch (Exception e) {

            var paymentEvent = new PaymentEvent(
                    paymentSaved.getOrderId(),
                    paymentSaved.getPaymentId().toString(),
                    paymentSaved.getTotalAmount(),
                    LocalDateTime.now());

            eventGateway.publishPaymentPending(paymentEvent);

        }
    }
}
