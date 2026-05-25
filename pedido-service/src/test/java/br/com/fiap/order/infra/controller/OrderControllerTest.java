package br.com.fiap.order.infra.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.order.core.gateway.AuthenticatedUserGateway;
import br.com.fiap.order.core.usecase.CreateOrderUseCase;
import br.com.fiap.order.core.usecase.GetOrderByIdUseCase;
import br.com.fiap.order.core.usecase.ListCustomerOrdersUseCase;
import br.com.fiap.order.infra.controller.mapper.OrderResponseMapper;
import br.com.fiap.order.support.OrderTestFixtures;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OrderResponseMapper.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @MockBean
    private GetOrderByIdUseCase getOrderByIdUseCase;

    @MockBean
    private ListCustomerOrdersUseCase listCustomerOrdersUseCase;

    @MockBean
    private AuthenticatedUserGateway authenticatedUserGateway;

    @Test
    @DisplayName("POST /pedidos should return 201 with order body")
    void createOrderShouldReturn201() throws Exception {
        var order = OrderTestFixtures.sampleOrder();
        when(authenticatedUserGateway.getAuthenticatedUserId()).thenReturn(OrderTestFixtures.CUSTOMER_ID);
        when(createOrderUseCase.execute(eq(OrderTestFixtures.CUSTOMER_ID), eq(OrderTestFixtures.RESTAURANT_ID), any()))
                .thenReturn(order);

        var requestBody = """
                {
                  "restaurantId": "rest-001",
                  "itens": [
                    {
                      "productId": 1,
                      "name": "X-Burger",
                      "quantity": 2,
                      "price": 25.90
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/pedidos/" + OrderTestFixtures.ORDER_ID))
                .andExpect(jsonPath("$.id").value(OrderTestFixtures.ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.totalPrice").value(51.80));

        verify(createOrderUseCase).execute(eq(OrderTestFixtures.CUSTOMER_ID), eq(OrderTestFixtures.RESTAURANT_ID), any());
    }

    @Test
    @DisplayName("GET /pedidos/{id} should return 200")
    void getOrderByIdShouldReturn200() throws Exception {
        var order = OrderTestFixtures.sampleOrder();
        when(authenticatedUserGateway.getAuthenticatedUserId()).thenReturn(OrderTestFixtures.CUSTOMER_ID);
        when(getOrderByIdUseCase.execute(OrderTestFixtures.ORDER_ID, OrderTestFixtures.CUSTOMER_ID))
                .thenReturn(order);

        mockMvc.perform(get("/pedidos/{id}", OrderTestFixtures.ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OrderTestFixtures.ORDER_ID.toString()))
                .andExpect(jsonPath("$.clientId").value(OrderTestFixtures.CUSTOMER_ID.toString()));
    }

    @Test
    @DisplayName("GET /pedidos should return customer orders")
    void listOrdersShouldReturn200() throws Exception {
        when(authenticatedUserGateway.getAuthenticatedUserId()).thenReturn(OrderTestFixtures.CUSTOMER_ID);
        when(listCustomerOrdersUseCase.execute(OrderTestFixtures.CUSTOMER_ID))
                .thenReturn(List.of(OrderTestFixtures.sampleOrder()));

        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(1))
                .andExpect(jsonPath("$.orders[0].status").value("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    @DisplayName("POST /pedidos should return 400 when validation fails")
    void createOrderShouldReturn400WhenInvalid() throws Exception {
        when(authenticatedUserGateway.getAuthenticatedUserId()).thenReturn(OrderTestFixtures.CUSTOMER_ID);

        var requestBody = """
                {
                  "restaurantId": "",
                  "itens": []
                }
                """;

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
