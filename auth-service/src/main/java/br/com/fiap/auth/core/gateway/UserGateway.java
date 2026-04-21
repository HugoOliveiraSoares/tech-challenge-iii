package br.com.fiap.auth.core.gateway;

import br.com.fiap.auth.core.domain.User;

public interface UserGateway {
    User save(User user);

    boolean existsByEmail(String email);
}
