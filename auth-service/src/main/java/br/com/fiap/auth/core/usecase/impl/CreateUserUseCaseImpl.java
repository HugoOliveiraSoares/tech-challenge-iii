package br.com.fiap.auth.core.usecase.impl;

import br.com.fiap.auth.core.domain.User;
import br.com.fiap.auth.core.dto.CreateUserInput;
import br.com.fiap.auth.core.dto.CreateUserOutput;
import br.com.fiap.auth.core.exception.EmailAlreadyInUseException;
import br.com.fiap.auth.core.gateway.UserGateway;
import br.com.fiap.auth.core.usecase.CreateUserUseCase;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserGateway userGateway;

    @Override
    public CreateUserOutput createUser(CreateUserInput input) {

        var email = input.email().toLowerCase().trim();
        if(userGateway.existsByEmail(email)){
            throw new EmailAlreadyInUseException("Email %s is already in use".formatted(email));
        }

        var newUser = new User(input.name(),
                email,
                input.password(),
                input.role());

        var saved = userGateway.save(newUser);

        return CreateUserOutput.from(saved);
    }
}
