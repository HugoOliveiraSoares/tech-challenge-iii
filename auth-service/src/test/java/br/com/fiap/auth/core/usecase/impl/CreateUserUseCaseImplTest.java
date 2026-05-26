package br.com.fiap.auth.core.usecase.impl;

import br.com.fiap.auth.core.domain.User;
import br.com.fiap.auth.core.exception.EmailAlreadyInUseException;
import br.com.fiap.auth.core.gateway.UserGateway;
import br.com.fiap.auth.support.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseImplTest {

    @Mock
    private UserGateway userGateway;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateUserUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("should create user with normalized email and encoded password")
    void shouldCreateUser() {
        var input = AuthTestFixtures.createUserInput();
        when(userGateway.existsByEmail(AuthTestFixtures.EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(AuthTestFixtures.PASSWORD)).thenReturn(AuthTestFixtures.ENCODED_PASSWORD);
        when(userGateway.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.createUser(input);

        verify(userGateway).save(userCaptor.capture());
        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo(AuthTestFixtures.NAME);
        assertThat(savedUser.getEmail()).isEqualTo(AuthTestFixtures.EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo(AuthTestFixtures.ENCODED_PASSWORD);
        assertThat(savedUser.getRole().name()).isEqualTo(AuthTestFixtures.ROLE);
        assertThat(result.id()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("should trim and lowercase email before checking duplicates")
    void shouldNormalizeEmailBeforeCheckingDuplicates() {
        var input = new br.com.fiap.auth.core.dto.CreateUserInput(
                AuthTestFixtures.NAME,
                "  JOAO@EMAIL.COM  ",
                AuthTestFixtures.PASSWORD,
                AuthTestFixtures.ROLE);
        when(userGateway.existsByEmail(AuthTestFixtures.EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(AuthTestFixtures.PASSWORD)).thenReturn(AuthTestFixtures.ENCODED_PASSWORD);
        when(userGateway.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.createUser(input);

        verify(userGateway).existsByEmail(AuthTestFixtures.EMAIL);
        verify(userGateway).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo(AuthTestFixtures.EMAIL);
    }

    @Test
    @DisplayName("should throw when email already exists")
    void shouldThrowWhenEmailAlreadyExists() {
        when(userGateway.existsByEmail(AuthTestFixtures.EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> useCase.createUser(AuthTestFixtures.createUserInput()))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessage("Email 'joao@email.com' is already in use");

        verify(passwordEncoder, never()).encode(any());
        verify(userGateway, never()).save(any());
    }
}
