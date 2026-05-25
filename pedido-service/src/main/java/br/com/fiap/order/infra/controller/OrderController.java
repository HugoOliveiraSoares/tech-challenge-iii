package br.com.fiap.order.infra.controller;

import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.core.gateway.AuthenticatedUserGateway;
import br.com.fiap.order.core.usecase.CreateOrderUseCase;
import br.com.fiap.order.core.usecase.GetOrderByIdUseCase;
import br.com.fiap.order.core.usecase.ListCustomerOrdersUseCase;
import br.com.fiap.order.infra.controller.dto.CreateOrderRequest;
import br.com.fiap.order.infra.controller.dto.OrderListResponse;
import br.com.fiap.order.infra.controller.dto.OrderResponse;
import br.com.fiap.order.infra.controller.mapper.OrderResponseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final ListCustomerOrdersUseCase listCustomerOrdersUseCase;
    private final AuthenticatedUserGateway authenticatedUserGateway;
    private final OrderResponseMapper orderResponseMapper;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        UUID customerId = authenticatedUserGateway.getAuthenticatedUserId();
        List<OrderItem> items = request.itens().stream()
                .map(item -> new OrderItem(
                        item.productId(),
                        item.name(),
                        item.quantity(),
                        item.price()))
                .toList();

        var order = createOrderUseCase.execute(customerId, request.restaurantId(), items);
        OrderResponse response = orderResponseMapper.toResponse(order);
        URI location = URI.create("/pedidos/" + order.getId());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        UUID customerId = authenticatedUserGateway.getAuthenticatedUserId();
        var order = getOrderByIdUseCase.execute(id, customerId);
        return ResponseEntity.ok(orderResponseMapper.toResponse(order));
    }

    @GetMapping
    public ResponseEntity<OrderListResponse> listCustomerOrders() {
        UUID customerId = authenticatedUserGateway.getAuthenticatedUserId();
        var orders = listCustomerOrdersUseCase.execute(customerId);
        return ResponseEntity.ok(orderResponseMapper.toListResponse(orders));
    }
}
