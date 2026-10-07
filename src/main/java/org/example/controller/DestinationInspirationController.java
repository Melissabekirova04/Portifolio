package org.example.controller;

import io.javalin.Javalin;
import org.example.service.DestinationInspirationService;

public class DestinationInspirationController {

    private final DestinationInspirationService service =
            new DestinationInspirationService();

    public void registerRoutes(Javalin app) {

        app.get("/api/destinations/{airportCode}", ctx -> {
            String airportCode = ctx.pathParam("airportCode");

            ctx.json(service.getDetails(airportCode));
        });
    }
}