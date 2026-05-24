package br.com.fiap.auth.infra.controller;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OpenIdController{

    private final JWKSet jwkSet;

    @Value("${jwt.issuer}")
    private String issuer;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks(){
        return jwkSet.toJSONObject();
    }

    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> openIdConfiguration(){
        return Map.of(
            "issuer", issuer,
            "jwks_uri", issuer + "/.well-known/jwks.json",
            "id_token_signing_alg_values_supported", new String[]{"RS256"},
                "token_endpoint_auth_methods_supported", new String[]{"client_secret_basic"}
        );
    }
}
