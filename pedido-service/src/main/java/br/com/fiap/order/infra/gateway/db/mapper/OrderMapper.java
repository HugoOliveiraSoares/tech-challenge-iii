package br.com.fiap.order.infra.gateway.db.mapper;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.infra.gateway.db.entity.OrderEntity;
import br.com.fiap.order.infra.gateway.db.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public Order toDomain(OrderEntity entity) {
        Order order = new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getRestaurantId(),
                mapItems(entity.getItems())
        );
        order.setStatus(entity.getStatus());
        order.setTotalAmount(entity.getTotalAmount());
        order.setCreatedAt(entity.getCreatedAt());
        return order;
    }

    public OrderEntity toEntity(Order domain) {
        OrderEntity entity = new OrderEntity();
        entity.setId(domain.getId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setRestaurantId(domain.getRestaurantId());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getCreatedAt());
        entity.setItems(mapItemEntities(domain.getItems()));
        return entity;
    }

    private List<OrderItem> mapItems(List<OrderItemEntity> entities) {
        return entities.stream()
                .map(item -> new OrderItem(
                        item.getProductId(),
                        item.getName(),
                        item.getQuantity(),
                        item.getPrice()))
                .toList();
    }

    private List<OrderItemEntity> mapItemEntities(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderItemEntity(
                        null,
                        null,
                        item.getProductId(),
                        item.getName(),
                        item.getQuantity(),
                        item.getPrice()))
                .toList();
    }
}
