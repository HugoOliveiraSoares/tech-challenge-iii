package br.com.fiap.auth.core.domain;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class User {
    private UUID id;
    private String name;
    private String email;
    private String password;
    private UserRole role;
    private LocalDateTime createdAt;

    public User(UUID id, String name, String email, String password, UserRole role, LocalDateTime createdAt) {
        this.setId(id);
        this.setName(name);
        this.setEmail(email);
        this.setPassword(password);
        this.setRole(role);
        this.setCreatedAt(createdAt);
    }

    public User(String name, String email, String password, UserRole role) {
        this.setId(UUID.randomUUID());
        this.setName(name);
        this.setEmail(email);
        this.setPassword(password);
        this.setRole(role);
        this.setCreatedAt(LocalDateTime.now());
    }

    private void setId(UUID id) {
        if(id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        this.id = id;
    }

    private void setName(String name) {
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.name = name;
    }

    private void setEmail(String email) {
        if(email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        this.email = email;
    }

    private void setPassword(String password) {
        if(password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        this.password = password;
    }

    private void setRole(UserRole role) {
        if(role == null) {
            throw new IllegalArgumentException("Role cannot be null or blank");
        }
        this.role = role;
    }

    private void setCreatedAt(LocalDateTime createdAt) {
        if(createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        this.createdAt = createdAt;
    }
}
