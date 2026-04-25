package br.com.fiap.auth.core.gateway;

import br.com.fiap.auth.core.domain.User;

import java.util.Optional;

public interface UserGateway {
    User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
