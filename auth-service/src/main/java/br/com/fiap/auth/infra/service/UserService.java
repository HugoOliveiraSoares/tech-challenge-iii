package br.com.fiap.auth.infra.service;

import br.com.fiap.auth.core.dto.CreateUserInput;
import br.com.fiap.auth.core.dto.CreateUserOutput;
import br.com.fiap.auth.core.usecase.CreateUserUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserService {

    private final CreateUserUseCase createUserUseCase;

    @Transactional
    public CreateUserOutput createUser(CreateUserInput input) {
        return createUserUseCase.createUser(input);
    }
}
