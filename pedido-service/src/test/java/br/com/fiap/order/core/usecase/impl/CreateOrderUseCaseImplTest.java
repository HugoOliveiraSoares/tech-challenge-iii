package br.com.fiap.order.core.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.core.exception.InvalidOrderException;
import br.com.fiap.order.core.gateway.OrderEventGateway;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.support.OrderTestFixtures;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseImplTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private OrderEventGateway orderEventGateway;

    @InjectMocks
    private CreateOrderUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    @DisplayName("should save order, publish event and return saved order")
    void shouldCreateOrderAndPublishEvent() {
        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = useCase.execute(
                OrderTestFixtures.CUSTOMER_ID,
                OrderTestFixtures.RESTAURANT_ID,
                OrderTestFixtures.sampleItems());

        verify(orderGateway).save(orderCaptor.capture());
        verify(orderEventGateway).publishOrderCreated(result);

        assertThat(result.getCustomerId()).isEqualTo(OrderTestFixtures.CUSTOMER_ID);
        assertThat(result.getRestaurantId()).isEqualTo(OrderTestFixtures.RESTAURANT_ID);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(51.80));
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should throw when customer id is null")
        void shouldThrowWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> useCase.execute((UUID) null, "rest", OrderTestFixtures.sampleItems()))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessage("Customer id is required");

            verifyNoInteractions(orderGateway, orderEventGateway);
        }

        @Test
        @DisplayName("should throw when restaurant id is blank")
        void shouldThrowWhenRestaurantIdIsBlank() {
            assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.CUSTOMER_ID, "  ", OrderTestFixtures.sampleItems()))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessage("Restaurant id is required");

            verifyNoInteractions(orderGateway, orderEventGateway);
        }

        @Test
        @DisplayName("should throw when items list is empty")
        void shouldThrowWhenItemsAreEmpty() {
            assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.CUSTOMER_ID, "rest", Collections.emptyList()))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessage("Order must contain at least one item");

            verifyNoInteractions(orderGateway, orderEventGateway);
        }

        @Test
        @DisplayName("should throw when product id is null")
        void shouldThrowWhenProductIdIsNull() {
            var invalidItem = new OrderItem(null, "Burger", 1, BigDecimal.TEN);

            assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.CUSTOMER_ID, "rest", List.of(invalidItem)))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("product id must be a positive number");

            verify(orderGateway, never()).save(any());
        }

        @Test
        @DisplayName("should throw when duplicate product ids")
        void shouldThrowWhenDuplicateProducts() {
            var items = List.of(
                    new OrderItem(1L, "A", 1, BigDecimal.TEN),
                    new OrderItem(1L, "B", 1, BigDecimal.ONE));

            assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.CUSTOMER_ID, "rest", items))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Duplicate product id");
        }

        @Test
        @DisplayName("should trim restaurant id and product name on create")
        void shouldNormalizeInputOnCreate() {
            when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            var items = List.of(new OrderItem(1L, "  Burger  ", 1, BigDecimal.valueOf(10)));

            Order result = useCase.execute(OrderTestFixtures.CUSTOMER_ID, "  rest-001  ", items);

            assertThat(result.getRestaurantId()).isEqualTo("rest-001");
            assertThat(result.getItems().get(0).getName()).isEqualTo("Burger");
        }

        @Test
        @DisplayName("should throw when quantity is zero")
        void shouldThrowWhenQuantityIsZero() {
            var invalidItem = new OrderItem(1L, "Burger", 0, BigDecimal.TEN);

            assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.CUSTOMER_ID, "rest", List.of(invalidItem)))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("quantity must be greater than zero");
        }

        @Test
        @DisplayName("should throw when price is zero")
        void shouldThrowWhenPriceIsZero() {
            var invalidItem = new OrderItem(1L, "Burger", 1, BigDecimal.ZERO);

            assertThatThrownBy(() -> useCase.execute(OrderTestFixtures.CUSTOMER_ID, "rest", List.of(invalidItem)))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("price must be greater than zero");
        }
    }
}
