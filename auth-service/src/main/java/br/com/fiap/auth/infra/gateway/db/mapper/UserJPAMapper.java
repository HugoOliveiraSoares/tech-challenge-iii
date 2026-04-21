package br.com.fiap.auth.infra.gateway.db.mapper;

import br.com.fiap.auth.core.domain.User;
import br.com.fiap.auth.infra.gateway.db.entity.UserEntity;

public class UserJPAMapper {
    private UserJPAMapper() {}

    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static User toDomain(UserEntity saved) {
        return new User(saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getPassword(),
                saved.getRole(),
                saved.getCreatedAt());
    }
}
