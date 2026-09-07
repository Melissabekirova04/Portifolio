package org.example.controller;

import io.javalin.Javalin;
import org.example.service.WeatherService;

public class WeatherController {

    private final WeatherService weatherService = new WeatherService();

    public void registerRoutes(Javalin app) {

        app.get("/api/weather/airports", ctx ->
                ctx.json(weatherService.getAllAirportWeather())
        );

        app.get("/api/weather/airports/{code}", ctx -> {

            String code = ctx.pathParam("code");

            try {
                ctx.json(
                        weatherService.getWeatherByAirportCode(code)
                );

            } catch (IllegalArgumentException e) {

                ctx.status(404);

                ctx.json(
                        new ErrorResponse(e.getMessage())
                );
            }
        });
    }

    private record ErrorResponse(String error) {
    }
}