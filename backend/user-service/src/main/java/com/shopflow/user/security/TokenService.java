package com.shopflow.user.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.shopflow.common.security.JwtProperties;
import com.shopflow.user.domain.UserAccount;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

/**
 * Pembuat JWT.
 *
 * <p>Isi token (claims):
 * <pre>
 * {
 *   "iss": "shopflow-identity",
 *   "sub": "42",                 // user id -> jadi header X-User-Id di hilir
 *   "email": "budi@example.com",
 *   "name": "Budi",
 *   "roles": ["CUSTOMER"],
 *   "iat": ..., "exp": ...
 * }
 * </pre>
 *
 * <p><b>Jangan pernah</b> menaruh data sensitif di JWT: payload-nya hanya di-encode
 * base64, siapa pun bisa membacanya. JWT menjamin keaslian, bukan kerahasiaan.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public TokenService(JwtProperties properties) {
        properties.requireUsable();
        SecretKey key = new SecretKeySpec(properties.secretBytes(), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        this.properties = properties;
    }

    public IssuedToken issue(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("roles", user.roleList())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(token, expiresAt, properties.accessTokenTtlSeconds());
    }

    public record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {
    }
}
