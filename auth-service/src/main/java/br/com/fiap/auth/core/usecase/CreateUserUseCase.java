package br.com.fiap.auth.core.usecase;

import br.com.fiap.auth.core.dto.CreateUserInput;
import br.com.fiap.auth.core.dto.CreateUserOutput;

public interface CreateUserUseCase {
    CreateUserOutput createUser(CreateUserInput input);
}
