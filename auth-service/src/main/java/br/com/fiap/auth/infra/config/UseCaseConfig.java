package br.com.fiap.auth.infra.config;

import br.com.fiap.auth.core.gateway.UserGateway;
import br.com.fiap.auth.core.usecase.CreateUserUseCase;
import br.com.fiap.auth.core.usecase.ValidateUserUseCase;
import br.com.fiap.auth.core.usecase.impl.CreateUserUseCaseImpl;
import br.com.fiap.auth.core.usecase.impl.ValidateUserUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateUserUseCase createUser(UserGateway userGateway, PasswordEncoder passwordEncoder) {
        return new CreateUserUseCaseImpl(userGateway, passwordEncoder);
    }

    @Bean
    public ValidateUserUseCase validateUser(UserGateway userGateway, PasswordEncoder passwordEncoder) {
        return new ValidateUserUseCaseImpl(userGateway, passwordEncoder);
    }
}
