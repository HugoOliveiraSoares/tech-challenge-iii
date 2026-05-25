package br.com.fiap.order.infra.gateway.db.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.infra.gateway.db.entity.OrderEntity;
import br.com.fiap.order.support.OrderTestFixtures;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper();

    @Test
    @DisplayName("should map OrderEntity to Order with all fields")
    void shouldMapEntityToDomain() {
        OrderEntity entity = OrderTestFixtures.sampleEntity(OrderStatus.PENDING_PAYMENT);

        Order result = orderMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(OrderTestFixtures.ORDER_ID);
        assertThat(result.getCustomerId()).isEqualTo(OrderTestFixtures.CUSTOMER_ID);
        assertThat(result.getRestaurantId()).isEqualTo(OrderTestFixtures.RESTAURANT_ID);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(51.80));
        assertThat(result.getCreatedAt()).isEqualTo(OrderTestFixtures.CREATED_AT);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("X-Burger");
    }

    @Test
    @DisplayName("should map Order to OrderEntity with all fields")
    void shouldMapDomainToEntity() {
        Order domain = OrderTestFixtures.sampleOrder(OrderStatus.PAID);

        OrderEntity result = orderMapper.toEntity(domain);

        assertThat(result.getId()).isEqualTo(OrderTestFixtures.ORDER_ID);
        assertThat(result.getCustomerId()).isEqualTo(OrderTestFixtures.CUSTOMER_ID);
        assertThat(result.getRestaurantId()).isEqualTo(OrderTestFixtures.RESTAURANT_ID);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(domain.getTotalAmount());
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("should throw NullPointerException when entity is null")
    void shouldThrowNPEWhenEntityIsNull() {
        assertThatThrownBy(() -> orderMapper.toDomain(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should throw NullPointerException when domain is null")
    void shouldThrowNPEWhenDomainIsNull() {
        assertThatThrownBy(() -> orderMapper.toEntity(null))
                .isInstanceOf(NullPointerException.class);
    }
}
