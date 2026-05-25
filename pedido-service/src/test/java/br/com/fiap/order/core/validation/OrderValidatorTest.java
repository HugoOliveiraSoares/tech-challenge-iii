package br.com.fiap.order.core.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.core.exception.InvalidOrderException;
import br.com.fiap.order.core.exception.UnauthorizedCustomerException;
import br.com.fiap.order.support.OrderTestFixtures;

class OrderValidatorTest {

    @Nested
    @DisplayName("create order")
    class CreateOrder {

        @Test
        @DisplayName("normalizeRestaurantId should trim value")
        void normalizeRestaurantIdShouldTrim() {
            assertThat(OrderValidator.normalizeRestaurantId("  rest-001  ")).isEqualTo("rest-001");
        }

        @Test
        @DisplayName("normalizeItems should trim product names")
        void normalizeItemsShouldTrimNames() {
            var items = List.of(new OrderItem(1L, "  Burger  ", 1, BigDecimal.TEN));

            var normalized = OrderValidator.normalizeItems(items);

            assertThat(normalized.get(0).getName()).isEqualTo("Burger");
        }

        @Test
        @DisplayName("should reject duplicate product ids")
        void shouldRejectDuplicateProducts() {
            var items = List.of(
                    new OrderItem(1L, "A", 1, BigDecimal.TEN),
                    new OrderItem(1L, "B", 1, BigDecimal.ONE));

            assertThatThrownBy(() -> OrderValidator.validateItems(items))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Duplicate product id");
        }

        @Test
        @DisplayName("should reject quantity above maximum")
        void shouldRejectExcessiveQuantity() {
            var item = new OrderItem(1L, "Burger", OrderValidator.MAX_ITEM_QUANTITY + 1, BigDecimal.TEN);

            assertThatThrownBy(() -> OrderValidator.validateItems(List.of(item)))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("quantity must not exceed");
        }

        @Test
        @DisplayName("should reject price with more than two decimal places")
        void shouldRejectInvalidPriceScale() {
            var item = new OrderItem(1L, "Burger", 1, new BigDecimal("10.999"));

            assertThatThrownBy(() -> OrderValidator.validateItems(List.of(item)))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("decimal places");
        }

        @Test
        @DisplayName("should reject null item in list")
        void shouldRejectNullItem() {
            var items = new ArrayList<OrderItem>();
            items.add(null);

            assertThatThrownBy(() -> OrderValidator.validateItems(items))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("position 0");
        }

        @Test
        @DisplayName("should reject more than max items")
        void shouldRejectTooManyItems() {
            var items = IntStream.rangeClosed(1, OrderValidator.MAX_ITEMS_PER_ORDER + 1)
                    .mapToObj(i -> new OrderItem((long) i, "Item " + i, 1, BigDecimal.ONE))
                    .toList();

            assertThatThrownBy(() -> OrderValidator.validateItems(items))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("must not exceed " + OrderValidator.MAX_ITEMS_PER_ORDER);
        }
    }

    @Nested
    @DisplayName("status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("canTransitionToPaid allows CREATED and PENDING_PAYMENT")
        void canTransitionToPaid() {
            assertThat(OrderValidator.canTransitionToPaid(OrderStatus.CREATED)).isTrue();
            assertThat(OrderValidator.canTransitionToPaid(OrderStatus.PENDING_PAYMENT)).isTrue();
            assertThat(OrderValidator.canTransitionToPaid(OrderStatus.PAID)).isFalse();
        }

        @Test
        @DisplayName("canTransitionToPendingPayment allows only CREATED")
        void canTransitionToPendingPayment() {
            assertThat(OrderValidator.canTransitionToPendingPayment(OrderStatus.CREATED)).isTrue();
            assertThat(OrderValidator.canTransitionToPendingPayment(OrderStatus.PENDING_PAYMENT)).isFalse();
            assertThat(OrderValidator.canTransitionToPendingPayment(OrderStatus.PAID)).isFalse();
        }
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("validateOwnership should pass for order owner")
        void shouldPassForOwner() {
            Order order = OrderTestFixtures.sampleOrder();

            OrderValidator.validateOwnership(order, OrderTestFixtures.CUSTOMER_ID);
        }

        @Test
        @DisplayName("validateOwnership should throw for different customer")
        void shouldThrowForDifferentCustomer() {
            Order order = OrderTestFixtures.sampleOrder();
            UUID other = UUID.fromString("33333333-3333-3333-3333-333333333333");

            assertThatThrownBy(() -> OrderValidator.validateOwnership(order, other))
                    .isInstanceOf(UnauthorizedCustomerException.class);
        }
    }
}
