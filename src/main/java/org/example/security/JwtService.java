package org.example.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import io.javalin.http.UnauthorizedResponse;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class JwtService {

    public static final long TOKEN_LIFETIME_SECONDS = 3600;

    private static final String ISSUER = "travel-planner";
    private static final String AUDIENCE = "travel-planner-api";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService() {
        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET mangler i miljøvariablerne."
            );
        }

        byte[] key;

        try {
            key = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "JWT_SECRET skal være Base64-kodet."
            );
        }

        if (key.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET skal indeholde mindst 32 tilfældige bytes."
            );
        }

        algorithm = Algorithm.HMAC256(key);

        verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withClaimPresence("sub")
                .withClaimPresence("exp")
                .build();
    }

    public String createToken(Long userId) {
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withSubject(userId.toString())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(now)
                .withExpiresAt(
                        now.plusSeconds(TOKEN_LIFETIME_SECONDS)
                )
                .sign(algorithm);
    }

    public Long verifyAndGetUserId(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);

            long userId = Long.parseLong(jwt.getSubject());

            if (userId <= 0) {
                throw new NumberFormatException();
            }

            return userId;

        } catch (JWTVerificationException | NumberFormatException e) {
            throw new UnauthorizedResponse(
                    "Tokenet er ugyldigt eller udløbet. Log ind igen."
            );
        }
    }
}