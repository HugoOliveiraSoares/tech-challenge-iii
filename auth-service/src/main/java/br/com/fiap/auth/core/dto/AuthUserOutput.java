package br.com.fiap.auth.core.dto;

import java.util.UUID;

public record AuthUserOutput(UUID userId,
                             String role) {
}
