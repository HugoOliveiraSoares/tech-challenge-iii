package br.com.fiap.auth.infra.security;

import br.com.fiap.auth.support.AuthTestFixtures;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private RSAKeyProvider rsaKeyProvider;

    @InjectMocks
    private JwtService jwtService;

    @Test
    @DisplayName("should generate signed jwt with subject issuer role and expiration")
    void shouldGenerateToken() throws Exception {
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();
        var privateKey = (RSAPrivateKey) keyPair.getPrivate();
        var publicKey = (RSAPublicKey) keyPair.getPublic();
        when(rsaKeyProvider.getPrivateKey()).thenReturn(privateKey);
        ReflectionTestUtils.setField(jwtService, "issuer", "auth-service-test");
        ReflectionTestUtils.setField(jwtService, "expiration", 60_000L);

        var token = jwtService.generateToken(AuthTestFixtures.authUserOutput());

        var claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo(AuthTestFixtures.USER_ID.toString());
        assertThat(claims.getIssuer()).isEqualTo("auth-service-test");
        assertThat(claims.get("role", String.class)).isEqualTo(AuthTestFixtures.ROLE);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
