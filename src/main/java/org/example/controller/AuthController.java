package org.example.controller;

import com.password4j.Password;
import io.javalin.Javalin;
import org.example.dao.UserDAO;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.User;

import java.util.Locale;
import java.util.Map;

public class AuthController {

    private final UserDAO userDAO = new UserDAO();

    public void registerRoutes(Javalin app) {

        app.post("/api/auth/register", ctx -> {
            RegisterRequest request = ctx.bodyAsClass(RegisterRequest.class);

            if (request.name() == null || request.name().isBlank()
                    || request.email() == null
                    || !request.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                    || request.password() == null
                    || request.password().length() < 8) {

                ctx.status(400).json(Map.of(
                        "error", "Angiv navn, gyldig e-mail og en adgangskode på mindst 8 tegn"
                ));
                return;
            }

            String email = request.email().trim().toLowerCase(Locale.ROOT);

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
                    new User(request.name().trim(), email, passwordHash)
            );

            ctx.status(201).json(Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail()
            ));
        });

        app.post("/api/auth/login", ctx -> {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);

            if (request.email() == null || request.password() == null) {
                ctx.status(400).json(Map.of(
                        "error", "E-mail og adgangskode mangler"
                ));
                return;
            }

            String email = request.email().trim().toLowerCase(Locale.ROOT);
            User user = userDAO.findByEmail(email).orElse(null);

            if (user == null
                    || !Password.check(request.password(), user.getPasswordHash())
                    .withBcrypt()) {

                ctx.status(401).json(Map.of(
                        "error", "Forkert e-mail eller adgangskode"
                ));
                return;
            }

            ctx.req().getSession(true);
            ctx.req().changeSessionId();
            ctx.sessionAttribute("userId", user.getId());

            ctx.json(Map.of(
                    "message", "Du er logget ind",
                    "name", user.getName()
            ));
        });

        app.post("/api/auth/logout", ctx -> {
            var session = ctx.req().getSession(false);

            if (session != null) {
                session.invalidate();
            }

            ctx.json(Map.of("message", "Du er logget ud"));
        });

        // Et beskyttet endpoint, så vi kan teste login
        app.get("/api/auth/me", ctx -> {
            Long userId = ctx.sessionAttribute("userId");

            if (userId == null) {
                ctx.status(401).json(Map.of(
                        "error", "Du skal logge ind først"
                ));
                return;
            }

            ctx.json(Map.of("userId", userId));
        });
    }
}