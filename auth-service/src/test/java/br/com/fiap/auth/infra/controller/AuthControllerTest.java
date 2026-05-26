package br.com.fiap.auth.infra.controller;

import br.com.fiap.auth.core.dto.CreateUserOutput;
import br.com.fiap.auth.infra.security.JwtService;
import br.com.fiap.auth.infra.service.UserService;
import br.com.fiap.auth.support.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /auth/register should return 201 with created user")
    void registerShouldReturn201() throws Exception {
        when(userService.createUser(any())).thenReturn(new CreateUserOutput(AuthTestFixtures.USER_ID));

        var requestBody = """
                {
                  "name": "Joao Silva",
                  "email": "joao@email.com",
                  "password": "senha123",
                  "role": "CLIENT"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/auth/users/" + AuthTestFixtures.USER_ID))
                .andExpect(jsonPath("$.id").value(AuthTestFixtures.USER_ID.toString()));

        verify(userService).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register should return 400 when validation fails")
    void registerShouldReturn400WhenValidationFails() throws Exception {
        var requestBody = """
                {
                  "name": "",
                  "email": "invalid-email",
                  "password": "123",
                  "role": ""
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login should return token")
    void loginShouldReturnToken() throws Exception {
        when(userService.validateUserCredentials(any())).thenReturn(AuthTestFixtures.authUserOutput());
        when(jwtService.generateToken(AuthTestFixtures.authUserOutput())).thenReturn("jwt-token");

        var requestBody = """
                {
                  "email": "joao@email.com",
                  "password": "senha123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(userService).validateUserCredentials(any());
        verify(jwtService).generateToken(AuthTestFixtures.authUserOutput());
    }

    @Test
    @DisplayName("POST /auth/login should return 400 when validation fails")
    void loginShouldReturn400WhenValidationFails() throws Exception {
        var requestBody = """
                {
                  "email": "invalid-email",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
