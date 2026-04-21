package br.com.fiap.auth.core.dto;

import br.com.fiap.auth.core.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserInput(@NotNull String name,
                              @NotNull @Email String email,
                              @NotNull @Size(min = 6) String password,
                              @NotNull UserRole role) {
}
