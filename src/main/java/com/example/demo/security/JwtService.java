package com.example.demo.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JwtService {
    public record Token(String value, Instant expiry) {}

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long accessSeconds;
    private final long refreshSeconds;

    JwtService(@Value("${JWT_SECRET}") String secret, @Value("${JWT_ACCESS_SECONDS:900}") long accessSeconds,
               @Value("${JWT_REFRESH_SECONDS:604800}") long refreshSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32 || accessSeconds <= 0 || refreshSeconds <= 0)
            throw new IllegalArgumentException("JWT requires a 32-byte secret and positive lifetimes");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
    }

    public Token createAccess(Long id) { return create(id, "access", accessSeconds); }
    public Token createRefresh(Long id) { return create(id, "refresh", refreshSeconds); }
    public Long verifyAccess(String token) { return verify(token, "access"); }
    public Long verifyRefresh(String token) { return verify(token, "refresh"); }

    private Token create(Long id, String type, long seconds) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiry = now.plusSeconds(seconds);
        var claims = JwtClaimsSet.builder().issuer("rbpo-lab5").subject(id.toString()).id(UUID.randomUUID().toString())
                .issuedAt(now).expiresAt(expiry).claim("type", type).build();
        var params = JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims);
        return new Token(encoder.encode(params).getTokenValue(), expiry);
    }

    private Long verify(String value, String type) {
        if (value.isBlank() || value.length() > 8192) throw new BadJwtException("Invalid token");
        Jwt jwt = decoder.decode(value);
        Instant now = Instant.now();
        if (!"rbpo-lab5".equals(jwt.getClaimAsString("iss")) || !type.equals(jwt.getClaims().get("type"))
                || jwt.getId() == null || jwt.getId().isBlank() || jwt.getSubject() == null
                || jwt.getIssuedAt() == null || jwt.getIssuedAt().isAfter(now) || jwt.getExpiresAt() == null
                || !jwt.getExpiresAt().isAfter(now) || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt()))
            throw new BadJwtException("Invalid claims");
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BadJwtException("Invalid subject");
        }
    }
}

