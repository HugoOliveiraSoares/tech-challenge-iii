package br.com.fiap.auth.infra.security;

import br.com.fiap.auth.core.dto.AuthUserOutput;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final RSAKeyProvider rsaKeyProvider;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(AuthUserOutput user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(user.userId().toString())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("role", user.role())
                .signWith(rsaKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }
}
