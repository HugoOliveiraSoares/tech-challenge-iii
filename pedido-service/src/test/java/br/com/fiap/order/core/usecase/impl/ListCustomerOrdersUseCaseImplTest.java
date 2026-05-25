package br.com.fiap.order.core.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.exception.InvalidOrderException;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.support.OrderTestFixtures;

@ExtendWith(MockitoExtension.class)
class ListCustomerOrdersUseCaseImplTest {

    @Mock
    private OrderGateway orderGateway;

    @InjectMocks
    private ListCustomerOrdersUseCaseImpl useCase;

    @Test
    @DisplayName("should return orders from gateway")
    void shouldReturnOrdersFromGateway() {
        List<Order> orders = List.of(OrderTestFixtures.sampleOrder());
        when(orderGateway.findByCustomerId(OrderTestFixtures.CUSTOMER_ID)).thenReturn(orders);

        List<Order> result = useCase.execute(OrderTestFixtures.CUSTOMER_ID);

        assertThat(result).isEqualTo(orders);
        verify(orderGateway).findByCustomerId(OrderTestFixtures.CUSTOMER_ID);
    }

    @Test
    @DisplayName("should return empty list when customer has no orders")
    void shouldReturnEmptyList() {
        when(orderGateway.findByCustomerId(OrderTestFixtures.CUSTOMER_ID)).thenReturn(List.of());

        List<Order> result = useCase.execute(OrderTestFixtures.CUSTOMER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should throw when customer id is null")
    void shouldThrowWhenCustomerIdIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Customer id is required");
    }
}
