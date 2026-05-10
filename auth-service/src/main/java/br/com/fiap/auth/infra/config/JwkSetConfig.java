package br.com.fiap.auth.infra.config;

import br.com.fiap.auth.infra.security.RSAKeyProvider;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JwkSetConfig {

    private final RSAKeyProvider rsaKeyProvider;

    @Bean
    public JWKSet jwkSet() {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeyProvider.getPublicKey())
                .keyID("auth-key")
                .algorithm(JWSAlgorithm.RS256)
                .build();

        return new JWKSet(rsaKey);
    }
}
