package br.com.fiap.order.infra.gateway.security;

import br.com.fiap.order.core.gateway.AuthenticatedUserGateway;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Lê o subject do JWT (UUID do cliente) do contexto de segurança. */
@Component
public class AuthenticatedUserGatewayImpl implements AuthenticatedUserGateway {

    @Override
    public UUID getAuthenticatedUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Authenticated user not found");
        }
        return UUID.fromString(jwt.getSubject());
    }
}
