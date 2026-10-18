package org.example.controller;

import com.password4j.Password;
import io.javalin.Javalin;
import org.example.dao.UserDAO;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.User;
import org.example.security.JwtService;
import org.example.security.SecurityMiddleware;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public class AuthController {

    private final UserDAO userDAO = new UserDAO();
    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void registerRoutes(Javalin app) {

        app.post("/api/auth/register", ctx -> {
            RegisterRequest request =
                    ctx.bodyAsClass(RegisterRequest.class);

            String email = request.email() == null
                    ? ""
                    : request.email().trim().toLowerCase(Locale.ROOT);

            if (request.name() == null
                    || request.name().isBlank()
                    || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                    || request.password() == null
                    || request.password().length() < 8) {

                ctx.status(400).json(Map.of(
                        "error",
                        "Angiv navn, gyldig e-mail og en adgangskode "
                                + "på mindst 8 tegn"
                ));
                return;
            }

            // BCrypt har en grænse på 72 bytes.
            if (request.password()
                    .getBytes(StandardCharsets.UTF_8).length > 72) {
                ctx.status(400).json(Map.of(
                        "error",
                        "Adgangskoden må højst fylde 72 bytes."
                ));
                return;
            }

            if (userDAO.findByEmail(email).isPresent()) {
                ctx.status(409).json(Map.of(
                        "error", "E-mailen er allerede registreret"
                ));
                return;
            }

            String passwordHash = Password.hash(request.password())
                    .withBcrypt()
                    .getResult();

            User user = userDAO.create(
                    new User(
                            request.name().trim(),
                            email,
                            passwordHash
                    )
            );

            ctx.status(201).json(Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail()
            ));
        });

        app.post("/api/auth/login", ctx -> {
            LoginRequest request =
                    ctx.bodyAsClass(LoginRequest.class);

            if (request.email() == null
                    || request.email().isBlank()
                    || request.password() == null
                    || request.password().isBlank()) {

                ctx.status(400).json(Map.of(
                        "error", "E-mail og adgangskode mangler"
                ));
                return;
            }

            String email = request.email()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            User user = userDAO.findByEmail(email).orElse(null);

            if (user == null
                    || !Password.check(
                    request.password(),
                    user.getPasswordHash()
            ).withBcrypt()) {

                ctx.status(401).json(Map.of(
                        "error", "Forkert e-mail eller adgangskode"
                ));
                return;
            }

            String token = jwtService.createToken(user.getId());

            SecurityMiddleware.bindDraftSession(ctx, user.getId());

            // Skift session-ID efter login.
            ctx.req().changeSessionId();

            ctx.header("Cache-Control", "no-store");

            ctx.json(Map.of(
                    "message", "Du er logget ind",
                    "name", user.getName(),
                    "token", token,
                    "tokenType", "Bearer",
                    "expiresIn", JwtService.TOKEN_LIFETIME_SECONDS
            ));
        });

        app.get("/api/auth/me", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);

            ctx.json(Map.of("userId", userId));
        });

        app.post("/api/auth/logout", ctx -> {
            SecurityMiddleware.requireUserId(ctx);

            var session = ctx.req().getSession(false);

            if (session != null) {
                session.invalidate();
            }

            ctx.json(Map.of(
                    "message",
                    "Rejsekladden er ryddet. Slet JWT-tokenet "
                            + "på klienten for at logge ud."
            ));
        });
    }
}