package br.com.fiap.order.infra.controller.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.support.OrderTestFixtures;

class OrderResponseMapperTest {

    private final OrderResponseMapper mapper = new OrderResponseMapper();

    @Test
    @DisplayName("toResponse should map order fields and API status")
    void toResponseShouldMapFields() {
        var order = OrderTestFixtures.sampleOrder(OrderStatus.PENDING_PAYMENT);

        var response = mapper.toResponse(order);

        assertThat(response.id()).isEqualTo(OrderTestFixtures.ORDER_ID);
        assertThat(response.clientId()).isEqualTo(OrderTestFixtures.CUSTOMER_ID);
        assertThat(response.restaurantId()).isEqualTo(OrderTestFixtures.RESTAURANT_ID);
        assertThat(response.status()).isEqualTo("PENDENTE_PAGAMENTO");
        assertThat(response.totalPrice()).isEqualByComparingTo(order.getTotalAmount());
        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().get(0).subtotal()).isEqualByComparingTo(BigDecimal.valueOf(51.80));
    }

    @Test
    @DisplayName("toListResponse should map order summaries")
    void toListResponseShouldMapSummaries() {
        var orders = List.of(
                OrderTestFixtures.sampleOrder(OrderStatus.CREATED),
                OrderTestFixtures.sampleOrder(OrderStatus.PAID));

        var response = mapper.toListResponse(orders);

        assertThat(response.orders()).hasSize(2);
        assertThat(response.orders().get(0).status()).isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(response.orders().get(1).status()).isEqualTo("PAGO");
    }

    @Test
    @DisplayName("toApiStatus should map all order statuses")
    void toApiStatusShouldMapAllStatuses() {
        assertThat(mapper.toApiStatus(OrderStatus.CREATED)).isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(mapper.toApiStatus(OrderStatus.PENDING_PAYMENT)).isEqualTo("PENDENTE_PAGAMENTO");
        assertThat(mapper.toApiStatus(OrderStatus.PAID)).isEqualTo("PAGO");
    }
}
