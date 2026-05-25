package br.com.fiap.order.infra.controller.mapper;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.infra.controller.dto.OrderItemResponse;
import br.com.fiap.order.infra.controller.dto.OrderListResponse;
import br.com.fiap.order.infra.controller.dto.OrderResponse;
import br.com.fiap.order.infra.controller.dto.OrderSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/** Converte entidades de domínio para DTOs da API (inclui nomes de status legíveis). */
@Component
public class OrderResponseMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                toApiStatus(order.getStatus()),
                mapItems(order.getItems()),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getCreatedAt()
        );
    }

    public OrderListResponse toListResponse(List<Order> orders) {
        List<OrderSummaryResponse> summaries = orders.stream()
                .map(order -> new OrderSummaryResponse(
                        order.getId(),
                        toApiStatus(order.getStatus()),
                        order.getTotalAmount(),
                        order.getCreatedAt()))
                .toList();
        return new OrderListResponse(summaries);
    }

    private List<OrderItemResponse> mapItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getSubtotal()))
                .toList();
    }

    /** Traduz enum interno para o status exibido na API (contrato do PDF). */
    public String toApiStatus(OrderStatus status) {
        return switch (status) {
            case CREATED -> "AGUARDANDO_PAGAMENTO";
            case PENDING_PAYMENT -> "PENDENTE_PAGAMENTO";
            case PAID -> "PAGO";
        };
    }
}
