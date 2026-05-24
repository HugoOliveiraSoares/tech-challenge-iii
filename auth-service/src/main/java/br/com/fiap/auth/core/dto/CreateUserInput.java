package br.com.fiap.auth.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserInput(@NotBlank
                              String name,
                              @NotBlank
                              @Email
                              String email,
                              @NotBlank
                              @Size(min = 6)
                              String password,
                              @NotBlank
                              String role) {
}
