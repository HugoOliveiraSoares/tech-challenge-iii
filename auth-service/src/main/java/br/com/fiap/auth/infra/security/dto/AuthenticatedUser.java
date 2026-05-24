package br.com.fiap.auth.infra.security.dto;

public record AuthenticatedUser(String userId,
                                String role) {
}
