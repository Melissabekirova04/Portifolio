package org.example.security;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;

public class SecurityMiddleware {

    private final JwtService jwtService;

    public SecurityMiddleware(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void register(Javalin app) {
        app.before(ctx -> {
            String path = ctx.path();

            boolean protectedPath =
                    isPathOrChild(path, "/api/trips")
                            || isPathOrChild(path, "/api/trip-draft")
                            || isPathOrChild(path, "/api/flights")
                            || isPathOrChild(path, "/api/auth/me")
                            || isPathOrChild(path, "/api/auth/logout");

            if (!protectedPath) {
                return;
            }

            String authorization = ctx.header("Authorization");

            if (authorization == null
                    || !authorization.regionMatches(
                    true, 0, "Bearer ", 0, 7
            )) {
                throw new UnauthorizedResponse(
                        "Send et JWT med Authorization: Bearer <token>"
                );
            }

            String token = authorization.substring(7).trim();

            if (token.isBlank()) {
                throw new UnauthorizedResponse("JWT-token mangler.");
            }

            Long userId = jwtService.verifyAndGetUserId(token);

            // Brugerens identitet gælder kun denne forespørgsel.
            ctx.attribute("authenticatedUserId", userId);

            // En kladde må ikke overtages ved skift af bruger.
            bindDraftSession(ctx, userId);
        });
    }

    public static Long requireUserId(Context ctx) {
        Long userId = ctx.attribute("authenticatedUserId");

        if (userId == null) {
            throw new UnauthorizedResponse(
                    "Du skal logge ind først."
            );
        }

        return userId;
    }

    public static void bindDraftSession(Context ctx, Long userId) {
        var session = ctx.req().getSession(false);

        if (session != null) {
            Object owner = session.getAttribute("draftOwnerId");

            if (!userId.equals(owner)) {
                session.invalidate();
            }
        }

        ctx.sessionAttribute("draftOwnerId", userId);
    }

    private boolean isPathOrChild(String path, String base) {
        return path.equals(base) || path.startsWith(base + "/");
    }
}