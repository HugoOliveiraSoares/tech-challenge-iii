package br.com.fiap.order.core.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    @Test
    @DisplayName("getSubtotal should multiply price by quantity")
    void getSubtotalShouldMultiplyPriceByQuantity() {
        OrderItem item = new OrderItem(1L, "X-Burger", 3, BigDecimal.valueOf(10));

        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }
}
