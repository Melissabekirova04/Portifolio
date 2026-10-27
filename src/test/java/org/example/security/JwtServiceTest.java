package org.example.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private Algorithm algorithm;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        byte[] key = Base64.getDecoder().decode(
                System.getenv("JWT_SECRET")
        );

        algorithm = Algorithm.HMAC256(key);
    }

    @Test
    void validTokenReturnsUserId() {
        String token = jwtService.createToken(2L);

        Long userId = jwtService.verifyAndGetUserId(token);

        assertEquals(Long.valueOf(2), userId);
    }

    @Test
    void rejectsInvalidToken() {
        assertThrows(
                UnauthorizedResponse.class,
                () -> jwtService.verifyAndGetUserId(
                        "dette-er-ikke-et-jwt"
                )
        );
    }

    @Test
    void rejectsExpiredToken() {
        String token = JWT.create()
                .withIssuer("travel-planner")
                .withAudience("travel-planner-api")
                .withSubject("2")
                .withIssuedAt(Instant.now().minusSeconds(7200))
                .withExpiresAt(Instant.now().minusSeconds(3600))
                .sign(algorithm);

        assertThrows(
                UnauthorizedResponse.class,
                () -> jwtService.verifyAndGetUserId(token)
        );
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        byte[] differentKey = Base64.getDecoder().decode(
                System.getenv("JWT_SECRET")
        );

        // Ændr én byte, så signeringsnøglen bliver en anden.
        differentKey[0] = (byte) (differentKey[0] ^ 1);

        String token = JWT.create()
                .withIssuer("travel-planner")
                .withAudience("travel-planner-api")
                .withSubject("2")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256(differentKey));

        assertThrows(
                UnauthorizedResponse.class,
                () -> jwtService.verifyAndGetUserId(token)
        );
    }

    @Test
    void rejectsTokenWithInvalidUserId() {
        String token = JWT.create()
                .withIssuer("travel-planner")
                .withAudience("travel-planner-api")
                .withSubject("0")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm);

        assertThrows(
                UnauthorizedResponse.class,
                () -> jwtService.verifyAndGetUserId(token)
        );
    }
}