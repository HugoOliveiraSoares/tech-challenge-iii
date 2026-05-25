package br.com.fiap.order.core.gateway;

import java.util.UUID;

public interface AuthenticatedUserGateway {
    UUID getAuthenticatedUserId();
}