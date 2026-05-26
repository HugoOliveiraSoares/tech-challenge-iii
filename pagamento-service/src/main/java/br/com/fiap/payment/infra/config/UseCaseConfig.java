package br.com.fiap.payment.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.payment.core.gateway.PaymentEventGateway;
import br.com.fiap.payment.core.gateway.PaymentGateway;
import br.com.fiap.payment.core.gateway.ProcPagGateway;
import br.com.fiap.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.payment.core.usecase.RetryPendingPaymentsUseCase;
import br.com.fiap.payment.core.usecase.impl.ProcessPaymentUseCaseImpl;
import br.com.fiap.payment.core.usecase.impl.RetryPendingPaymentsUseCaseImpl;

@Configuration
public class UseCaseConfig {

    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(
            PaymentGateway paymentGateway,
            ProcPagGateway procPagGateway,
            PaymentEventGateway paymentEventGateway) {
        return new ProcessPaymentUseCaseImpl(paymentGateway, procPagGateway, paymentEventGateway);
    }

    @Bean
    public RetryPendingPaymentsUseCase retryPendingPaymentsUseCase(
            PaymentGateway paymentGateway,
            ProcPagGateway procPagGateway,
            PaymentEventGateway paymentEventGateway) {
        return new RetryPendingPaymentsUseCaseImpl(paymentGateway, procPagGateway, paymentEventGateway);
    }
}
