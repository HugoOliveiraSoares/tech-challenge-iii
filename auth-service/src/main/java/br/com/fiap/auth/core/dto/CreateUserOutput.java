package br.com.fiap.auth.core.dto;

import br.com.fiap.auth.core.domain.User;

import java.util.UUID;

public record CreateUserOutput(UUID id) {

    public static CreateUserOutput from(User user) {
        return new CreateUserOutput(user.getId());
    }
}
