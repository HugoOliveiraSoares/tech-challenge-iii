package br.com.fiap.order.core.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.order.support.OrderTestFixtures;

class OrderTest {

    @Test
    @DisplayName("should calculate total amount from items")
    void shouldCalculateTotalFromItems() {
        var items = List.of(
                new OrderItem(1L, "Item A", 2, BigDecimal.valueOf(10)),
                new OrderItem(2L, "Item B", 1, BigDecimal.valueOf(5.50))
        );

        Order order = new Order(UUID.randomUUID(), OrderTestFixtures.CUSTOMER_ID, "rest-1", items);

        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.50));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("markAsPaid should set status to PAID")
    void markAsPaidShouldUpdateStatus() {
        Order order = OrderTestFixtures.sampleOrder();

        order.markAsPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("markAsPendingPayment should set status to PENDING_PAYMENT")
    void markAsPendingPaymentShouldUpdateStatus() {
        Order order = OrderTestFixtures.sampleOrder();

        order.markAsPendingPayment();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }
}
