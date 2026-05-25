package br.com.fiap.order.core.gateway;

import java.util.UUID;

/** Porta para obter o ID do cliente autenticado a partir do JWT. */
public interface AuthenticatedUserGateway {
    UUID getAuthenticatedUserId();
}