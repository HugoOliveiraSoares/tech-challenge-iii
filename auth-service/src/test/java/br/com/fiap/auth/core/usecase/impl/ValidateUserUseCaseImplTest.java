package br.com.fiap.auth.core.usecase.impl;

import br.com.fiap.auth.core.dto.UserCredentialsInput;
import br.com.fiap.auth.core.exception.InvalidCredentialsException;
import br.com.fiap.auth.core.gateway.UserGateway;
import br.com.fiap.auth.support.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateUserUseCaseImplTest {

    @Mock
    private UserGateway userGateway;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ValidateUserUseCaseImpl useCase;

    @Test
    @DisplayName("should validate credentials and return authenticated user")
    void shouldValidateCredentials() {
        when(userGateway.findByEmail(AuthTestFixtures.EMAIL)).thenReturn(Optional.of(AuthTestFixtures.user()));
        when(passwordEncoder.matches(AuthTestFixtures.PASSWORD, AuthTestFixtures.ENCODED_PASSWORD)).thenReturn(true);

        var result = useCase.validateUserCredentials(AuthTestFixtures.credentialsInput());

        assertThat(result.userId()).isEqualTo(AuthTestFixtures.USER_ID);
        assertThat(result.role()).isEqualTo(AuthTestFixtures.ROLE);
    }

    @Test
    @DisplayName("should normalize email before lookup")
    void shouldNormalizeEmailBeforeLookup() {
        var input = new UserCredentialsInput("  JOAO@EMAIL.COM  ", AuthTestFixtures.PASSWORD);
        when(userGateway.findByEmail(AuthTestFixtures.EMAIL)).thenReturn(Optional.of(AuthTestFixtures.user()));
        when(passwordEncoder.matches(AuthTestFixtures.PASSWORD, AuthTestFixtures.ENCODED_PASSWORD)).thenReturn(true);

        useCase.validateUserCredentials(input);

        verify(userGateway).findByEmail(AuthTestFixtures.EMAIL);
    }

    @Test
    @DisplayName("should throw when user is not found")
    void shouldThrowWhenUserIsNotFound() {
        when(userGateway.findByEmail(AuthTestFixtures.EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.validateUserCredentials(AuthTestFixtures.credentialsInput()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(InvalidCredentialsException.MESSAGE);
    }

    @Test
    @DisplayName("should throw when password does not match")
    void shouldThrowWhenPasswordDoesNotMatch() {
        when(userGateway.findByEmail(AuthTestFixtures.EMAIL)).thenReturn(Optional.of(AuthTestFixtures.user()));
        when(passwordEncoder.matches(AuthTestFixtures.PASSWORD, AuthTestFixtures.ENCODED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> useCase.validateUserCredentials(AuthTestFixtures.credentialsInput()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(InvalidCredentialsException.MESSAGE);
    }
}
