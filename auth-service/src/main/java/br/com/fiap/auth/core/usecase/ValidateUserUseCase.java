package br.com.fiap.auth.core.usecase;

import br.com.fiap.auth.core.dto.AuthUserOutput;
import br.com.fiap.auth.core.dto.UserCredentialsInput;

public interface ValidateUserUseCase {
    AuthUserOutput validateUserCredentials(UserCredentialsInput input);
}
