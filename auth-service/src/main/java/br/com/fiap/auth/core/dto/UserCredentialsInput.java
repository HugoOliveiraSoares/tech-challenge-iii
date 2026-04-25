package br.com.fiap.auth.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCredentialsInput(@NotNull
                                   @Email
                                   String email,
                                   @NotBlank
                                   String password) {
}
