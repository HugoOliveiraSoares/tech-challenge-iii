package br.com.fiap.auth.infra.gateway.db.repository;

import br.com.fiap.auth.infra.gateway.db.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserJPARepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmail(String email);
}
