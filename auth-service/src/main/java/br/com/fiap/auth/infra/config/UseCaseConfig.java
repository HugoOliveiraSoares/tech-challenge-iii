package br.com.fiap.auth.infra.config;

import br.com.fiap.auth.core.gateway.UserGateway;
import br.com.fiap.auth.core.usecase.CreateUserUseCase;
import br.com.fiap.auth.core.usecase.impl.CreateUserUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UseCaseConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CreateUserUseCase createUser(UserGateway userGateway, PasswordEncoder passwordEncoder) {
        return new CreateUserUseCaseImpl(userGateway, passwordEncoder);
    }
}
