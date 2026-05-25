package br.com.fiap.order.infra.gateway.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.infra.gateway.db.entity.OrderEntity;
import br.com.fiap.order.infra.gateway.db.mapper.OrderMapper;
import br.com.fiap.order.infra.gateway.db.repository.OrderEntityRepository;
import br.com.fiap.order.support.OrderTestFixtures;

@ExtendWith(MockitoExtension.class)
class OrderSpringDataGatewayTest {

    @Mock
    private OrderEntityRepository repository;

    private OrderSpringDataGateway gateway;

    private OrderEntity entity;
    private Order domain;

    @BeforeEach
    void setUp() {
        entity = OrderTestFixtures.sampleEntity();
        domain = OrderTestFixtures.sampleOrder();
        gateway = new OrderSpringDataGateway(repository, new OrderMapper());
    }

    @Test
    @DisplayName("should return mapped order when findById finds entity")
    void shouldReturnOrderWhenFindByIdFinds() {
        when(repository.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.of(entity));

        Optional<Order> result = gateway.findById(OrderTestFixtures.ORDER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(OrderTestFixtures.ORDER_ID);
        verify(repository).findById(OrderTestFixtures.ORDER_ID);
    }

    @Test
    @DisplayName("should return empty when findById does not find entity")
    void shouldReturnEmptyWhenFindByIdNotFound() {
        when(repository.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.empty());

        Optional<Order> result = gateway.findById(OrderTestFixtures.ORDER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should save new order when id does not exist")
    void shouldSaveNewOrder() {
        when(repository.findById(domain.getId())).thenReturn(Optional.empty());
        when(repository.save(any(OrderEntity.class))).thenReturn(entity);

        Order result = gateway.save(domain);

        assertThat(result.getId()).isEqualTo(OrderTestFixtures.ORDER_ID);
        verify(repository).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("should merge and save when order already exists")
    void shouldMergeExistingOrder() {
        Order paidOrder = OrderTestFixtures.sampleOrder(OrderStatus.PAID);
        when(repository.findById(OrderTestFixtures.ORDER_ID)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        Order result = gateway.save(paidOrder);

        verify(repository).save(entity);
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("should return customer orders")
    void shouldReturnCustomerOrders() {
        when(repository.findByCustomerIdOrderByCreatedAtDesc(OrderTestFixtures.CUSTOMER_ID))
                .thenReturn(List.of(entity));

        List<Order> result = gateway.findByCustomerId(OrderTestFixtures.CUSTOMER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(OrderTestFixtures.CUSTOMER_ID);
    }

    @Test
    @DisplayName("should return empty list when customer has no orders")
    void shouldReturnEmptyList() {
        when(repository.findByCustomerIdOrderByCreatedAtDesc(OrderTestFixtures.CUSTOMER_ID))
                .thenReturn(Collections.emptyList());

        List<Order> result = gateway.findByCustomerId(OrderTestFixtures.CUSTOMER_ID);

        assertThat(result).isEmpty();
    }
}
