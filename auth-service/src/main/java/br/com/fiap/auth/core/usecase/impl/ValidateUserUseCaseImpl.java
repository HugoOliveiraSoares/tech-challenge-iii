package br.com.fiap.auth.core.usecase.impl;

import br.com.fiap.auth.core.dto.AuthUserOutput;
import br.com.fiap.auth.core.dto.UserCredentialsInput;
import br.com.fiap.auth.core.exception.InvalidCredentialsException;
import br.com.fiap.auth.core.gateway.UserGateway;
import br.com.fiap.auth.core.usecase.ValidateUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class ValidateUserUseCaseImpl implements ValidateUserUseCase {
    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthUserOutput validateUserCredentials(UserCredentialsInput input) {
        var userByEmail = userGateway.findByEmail(input.email().toLowerCase().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(input.password(), userByEmail.getPassword())){
            throw new InvalidCredentialsException();
        }

        return new AuthUserOutput(userByEmail.getId(), userByEmail.getRole().name());
    }
}
