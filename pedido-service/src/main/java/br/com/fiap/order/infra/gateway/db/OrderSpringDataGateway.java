package br.com.fiap.order.infra.gateway.db;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.gateway.OrderGateway;
import br.com.fiap.order.infra.gateway.db.entity.OrderEntity;
import br.com.fiap.order.infra.gateway.db.entity.OrderItemEntity;
import br.com.fiap.order.infra.gateway.db.mapper.OrderMapper;
import br.com.fiap.order.infra.gateway.db.repository.OrderEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter JPA: implementa OrderGateway usando PostgreSQL. */
@Component
@RequiredArgsConstructor
public class OrderSpringDataGateway implements OrderGateway {

    private final OrderEntityRepository orderEntityRepository;
    private final OrderMapper orderMapper;

    @Override
    public Order save(Order order) {
        OrderEntity entity = orderEntityRepository.findById(order.getId())
                .map(existing -> merge(existing, order))
                .orElseGet(() -> orderMapper.toEntity(order));
        var saved = orderEntityRepository.save(entity);
        return orderMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return orderEntityRepository.findById(orderId).map(orderMapper::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return orderEntityRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(orderMapper::toDomain)
                .toList();
    }

    /** Atualiza pedido existente sem duplicar itens no banco. */
    private OrderEntity merge(OrderEntity existing, Order order) {
        existing.setStatus(order.getStatus());
        existing.setTotalAmount(order.getTotalAmount());
        existing.getItems().clear();
        existing.getItems().addAll(mapItemEntities(order.getItems()));
        return existing;
    }

    private List<OrderItemEntity> mapItemEntities(List<br.com.fiap.order.core.domain.OrderItem> items) {
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
