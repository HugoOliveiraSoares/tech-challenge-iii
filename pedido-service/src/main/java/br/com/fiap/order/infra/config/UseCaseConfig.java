package br.com.fiap.order.infra.config;

import br.com.fiap.order.core.gateway.OrderEventGateway;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.core.usecase.CreateOrderUseCase;
import br.com.fiap.order.core.usecase.GetOrderByIdUseCase;
import br.com.fiap.order.core.usecase.ListCustomerOrdersUseCase;
import br.com.fiap.order.core.usecase.MarkOrderAsPaidUseCase;
import br.com.fiap.order.core.usecase.MarkOrderAsPendingPaymentUseCase;
import br.com.fiap.order.core.usecase.impl.CreateOrderUseCaseImpl;
import br.com.fiap.order.core.usecase.impl.GetOrderByIdUseCaseImpl;
import br.com.fiap.order.core.usecase.impl.ListCustomerOrdersUseCaseImpl;
import br.com.fiap.order.core.usecase.impl.MarkOrderAsPaidUseCaseImpl;
import br.com.fiap.order.core.usecase.impl.MarkOrderAsPendingPaymentUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra implementações dos casos de uso como beans Spring (core sem @Service). */
@Configuration
public class UseCaseConfig {

    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderGateway orderGateway, OrderEventGateway orderEventGateway) {
        return new CreateOrderUseCaseImpl(orderGateway, orderEventGateway);
    }

    @Bean
    public GetOrderByIdUseCase getOrderByIdUseCase(OrderGateway orderGateway) {
        return new GetOrderByIdUseCaseImpl(orderGateway);
    }

    @Bean
    public ListCustomerOrdersUseCase listCustomerOrdersUseCase(OrderGateway orderGateway) {
        return new ListCustomerOrdersUseCaseImpl(orderGateway);
    }

    @Bean
    public MarkOrderAsPaidUseCase markOrderAsPaidUseCase(OrderGateway orderGateway) {
        return new MarkOrderAsPaidUseCaseImpl(orderGateway);
    }

    @Bean
    public MarkOrderAsPendingPaymentUseCase markOrderAsPendingPaymentUseCase(OrderGateway orderGateway) {
        return new MarkOrderAsPendingPaymentUseCaseImpl(orderGateway);
    }
}
