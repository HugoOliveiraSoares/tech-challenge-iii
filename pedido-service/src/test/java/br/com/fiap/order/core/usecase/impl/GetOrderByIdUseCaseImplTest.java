package br.com.fiap.order.core.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.exception.InvalidOrderException;
import br.com.fiap.order.core.exception.OrderNotFoundException;
import br.com.fiap.order.core.exception.UnauthorizedCustomerException;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.support.OrderTestFixtures;

@ExtendWith(MockitoExtension.class)
class GetOrderByIdUseCaseImplTest {

    @Mock
    private OrderGateway orderGateway;

    @InjectMocks
    private GetOrderByIdUseCaseImpl useCase;

    @Test
    @DisplayName("should throw when order id is null")
    void shouldThrowWhenOrderIdIsNull() {
        assertThatThrownBy(() -> useCase.execute(null, OrderTestFixtures.CUSTOMER_ID))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Order id is required");
    }

    @Test
    @DisplayName("should return order when customer is owner")
    void shouldReturnOrderWhenCustomerIsOwner() {
        Order order = OrderTestFixtures.sampleOrder();
        when(orderGateway.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.of(order));

        Order result = useCase.execute(OrderTestFixtures.ORDER_ID, OrderTestFixtures.CUSTOMER_ID);

        assertThat(result).isEqualTo(order);
        verify(orderGateway).findById(OrderTestFixtures.ORDER_ID);
    }

    @Test
    @DisplayName("should throw OrderNotFoundException when order does not exist")
    void shouldThrowWhenOrderNotFound() {
        when(orderGateway.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.ORDER_ID, OrderTestFixtures.CUSTOMER_ID))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderGateway).findById(OrderTestFixtures.ORDER_ID);
    }

    @Test
    @DisplayName("should throw UnauthorizedCustomerException when customer is not owner")
    void shouldThrowWhenCustomerIsNotOwner() {
        Order order = OrderTestFixtures.sampleOrder();
        UUID otherCustomer = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(orderGateway.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.ORDER_ID, otherCustomer))
                .isInstanceOf(UnauthorizedCustomerException.class);
    }
}
