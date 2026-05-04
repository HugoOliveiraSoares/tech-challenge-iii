package br.com.fiap.auth.core.domain;

import br.com.fiap.auth.core.exception.DomainException;

public enum UserRole {
    CLIENT,
    OWNER;

    public static UserRole of(String role){
        if(role == null || role.isBlank()){
            throw new DomainException("Role cannot be null or blank");
        }

        try{
            return UserRole.valueOf(role.trim().toUpperCase());
        }catch(IllegalArgumentException e){
            throw new DomainException("Invalid user role: '%s'. Allowed values: CLIENT, OWNER".formatted(role));
        }
    }
}
