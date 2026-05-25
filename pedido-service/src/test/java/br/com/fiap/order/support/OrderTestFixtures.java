package br.com.fiap.order.support;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.infra.gateway.db.entity.OrderEntity;
import br.com.fiap.order.infra.gateway.db.entity.OrderItemEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class OrderTestFixtures {

    public static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final String RESTAURANT_ID = "rest-001";
    public static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 15, 10, 30);

    private OrderTestFixtures() {
    }

    public static OrderItem sampleItem() {
        return new OrderItem(1L, "X-Burger", 2, BigDecimal.valueOf(25.90));
    }

    public static List<OrderItem> sampleItems() {
        return List.of(sampleItem());
    }

    public static Order sampleOrder() {
        return sampleOrder(OrderStatus.CREATED);
    }

    public static Order sampleOrder(OrderStatus status) {
        Order order = new Order(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, sampleItems());
        order.setStatus(status);
        order.setCreatedAt(CREATED_AT);
        return order;
    }

    public static OrderEntity sampleEntity() {
        return sampleEntity(OrderStatus.CREATED);
    }

    public static OrderEntity sampleEntity(OrderStatus status) {
        OrderEntity entity = new OrderEntity();
        entity.setId(ORDER_ID);
        entity.setCustomerId(CUSTOMER_ID);
        entity.setRestaurantId(RESTAURANT_ID);
        entity.setTotalAmount(BigDecimal.valueOf(51.80));
        entity.setStatus(status);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(CREATED_AT);
        entity.setItems(List.of(new OrderItemEntity(
                1L,
                ORDER_ID,
                1L,
                "X-Burger",
                2,
                BigDecimal.valueOf(25.90))));
        return entity;
    }
}
