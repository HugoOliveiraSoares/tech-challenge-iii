package br.com.fiap.order.infra.gateway.db.repository;

import br.com.fiap.order.infra.gateway.db.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositório Spring Data JPA para a tabela orders. */
public interface OrderEntityRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
