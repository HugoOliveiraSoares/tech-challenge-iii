package br.com.fiap.auth.support;

import br.com.fiap.auth.core.domain.User;
import br.com.fiap.auth.core.domain.UserRole;
import br.com.fiap.auth.core.dto.AuthUserOutput;
import br.com.fiap.auth.core.dto.CreateUserInput;
import br.com.fiap.auth.core.dto.UserCredentialsInput;
import br.com.fiap.auth.infra.gateway.db.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AuthTestFixtures {

    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final String NAME = "Joao Silva";
    public static final String EMAIL = "joao@email.com";
    public static final String PASSWORD = "senha123";
    public static final String ENCODED_PASSWORD = "encoded-password";
    public static final String ROLE = "CLIENT";
    public static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 10, 0);

    private AuthTestFixtures() {
    }

    public static CreateUserInput createUserInput() {
        return new CreateUserInput(NAME, EMAIL, PASSWORD, ROLE);
    }

    public static UserCredentialsInput credentialsInput() {
        return new UserCredentialsInput(EMAIL, PASSWORD);
    }

    public static AuthUserOutput authUserOutput() {
        return new AuthUserOutput(USER_ID, ROLE);
    }

    public static User user() {
        return new User(USER_ID, NAME, EMAIL, ENCODED_PASSWORD, ROLE, CREATED_AT);
    }

    public static UserEntity userEntity() {
        return UserEntity.builder()
                .id(USER_ID)
                .name(NAME)
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .role(UserRole.CLIENT)
                .createdAt(CREATED_AT)
                .build();
    }
}
