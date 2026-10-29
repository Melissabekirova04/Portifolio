package org.example.controller;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import org.example.security.SecurityMiddleware;

import java.util.Map;

public class FlightSelectionController {

    public void registerRoutes(Javalin app) {
        app.delete("/api/trip-draft/flight", ctx -> {
            SecurityMiddleware.requireUserId(ctx);

            Object destination =
                    ctx.sessionAttribute("tripDestination");

            if (destination == null) {
                throw new BadRequestResponse(
                        "Du skal vælge en destination først."
                );
            }

            ctx.sessionAttribute("selectedFlight", null);

            ctx.json(Map.of(
                    "message", "Flyet er fjernet fra rejsekladden."
            ));
        });
    }
}