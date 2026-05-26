package br.com.fiap.auth.infra.gateway.db;

import br.com.fiap.auth.infra.gateway.db.entity.UserEntity;
import br.com.fiap.auth.infra.gateway.db.repository.UserJPARepository;
import br.com.fiap.auth.support.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private UserJPARepository userJPARepository;

    @InjectMocks
    private UserRepositoryAdapter adapter;

    @Captor
    private ArgumentCaptor<UserEntity> entityCaptor;

    @Test
    @DisplayName("should save mapped user entity and return domain")
    void shouldSaveUser() {
        when(userJPARepository.save(org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .thenReturn(AuthTestFixtures.userEntity());

        var result = adapter.save(AuthTestFixtures.user());

        verify(userJPARepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getId()).isEqualTo(AuthTestFixtures.USER_ID);
        assertThat(entityCaptor.getValue().getEmail()).isEqualTo(AuthTestFixtures.EMAIL);
        assertThat(result.getId()).isEqualTo(AuthTestFixtures.USER_ID);
        assertThat(result.getEmail()).isEqualTo(AuthTestFixtures.EMAIL);
        assertThat(result.getRole().name()).isEqualTo(AuthTestFixtures.ROLE);
    }

    @Test
    @DisplayName("should find user by email")
    void shouldFindByEmail() {
        when(userJPARepository.findByEmail(AuthTestFixtures.EMAIL))
                .thenReturn(Optional.of(AuthTestFixtures.userEntity()));

        var result = adapter.findByEmail(AuthTestFixtures.EMAIL);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(AuthTestFixtures.USER_ID);
        assertThat(result.get().getEmail()).isEqualTo(AuthTestFixtures.EMAIL);
    }

    @Test
    @DisplayName("should return empty when user by email is not found")
    void shouldReturnEmptyWhenFindByEmailDoesNotFind() {
        when(userJPARepository.findByEmail(AuthTestFixtures.EMAIL)).thenReturn(Optional.empty());

        var result = adapter.findByEmail(AuthTestFixtures.EMAIL);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should delegate exists by email")
    void shouldDelegateExistsByEmail() {
        when(userJPARepository.existsByEmail(AuthTestFixtures.EMAIL)).thenReturn(true);

        var result = adapter.existsByEmail(AuthTestFixtures.EMAIL);

        assertThat(result).isTrue();
        verify(userJPARepository).existsByEmail(AuthTestFixtures.EMAIL);
    }
}
