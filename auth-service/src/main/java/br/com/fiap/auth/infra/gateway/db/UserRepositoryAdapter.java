package br.com.fiap.auth.infra.gateway.db;

import br.com.fiap.auth.core.domain.User;
import br.com.fiap.auth.core.gateway.UserGateway;
import br.com.fiap.auth.infra.gateway.db.mapper.UserJPAMapper;
import br.com.fiap.auth.infra.gateway.db.repository.UserJPARepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@AllArgsConstructor
public class UserRepositoryAdapter implements UserGateway {

    private final UserJPARepository userJPARepository;

    @Override
    public User save(User user) {
        var saved = userJPARepository.save(UserJPAMapper.toEntity(user));

        return UserJPAMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJPARepository.findByEmail(email)
                .map(UserJPAMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJPARepository.existsByEmail(email);
    }

}
