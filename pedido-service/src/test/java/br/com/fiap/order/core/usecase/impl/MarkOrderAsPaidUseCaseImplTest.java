package br.com.fiap.order.core.usecase.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.core.exception.InvalidOrderException;
import br.com.fiap.order.core.exception.OrderNotFoundException;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.support.OrderTestFixtures;

@ExtendWith(MockitoExtension.class)
class MarkOrderAsPaidUseCaseImplTest {

    @Mock
    private OrderGateway orderGateway;

    @InjectMocks
    private MarkOrderAsPaidUseCaseImpl useCase;

    @Test
    @DisplayName("should mark order as paid and save")
    void shouldMarkOrderAsPaidAndSave() {
        Order order = OrderTestFixtures.sampleOrder();
        when(orderGateway.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.of(order));
        when(orderGateway.save(order)).thenReturn(order);

        useCase.execute(OrderTestFixtures.ORDER_ID);

        verify(orderGateway).save(order);
        org.assertj.core.api.Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("should not save when order is already paid")
    void shouldNotSaveWhenAlreadyPaid() {
        Order order = OrderTestFixtures.sampleOrder(OrderStatus.PAID);
        when(orderGateway.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.of(order));

        useCase.execute(OrderTestFixtures.ORDER_ID);

        verify(orderGateway, never()).save(any());
    }

    @Test
    @DisplayName("should throw when order id is null")
    void shouldThrowWhenOrderIdIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Order id is required");
    }

    @Test
    @DisplayName("should mark as paid from PENDING_PAYMENT status")
    void shouldMarkAsPaidFromPendingPayment() {
        Order order = OrderTestFixtures.sampleOrder(OrderStatus.PENDING_PAYMENT);
        when(orderGateway.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.of(order));
        when(orderGateway.save(order)).thenReturn(order);

        useCase.execute(OrderTestFixtures.ORDER_ID);

        org.assertj.core.api.Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("should throw when order not found")
    void shouldThrowWhenOrderNotFound() {
        when(orderGateway.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.ORDER_ID))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
