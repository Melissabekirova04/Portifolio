package org.example;

import io.javalin.Javalin;
import org.example.controller.WeatherController;

public class Main {

    public static void main(String[] args) {

        // Starter vores Javalin server
        Javalin app = Javalin.create().start(7070);

        // Opretter vores weather controller
        WeatherController weatherController = new WeatherController();

        // Registrerer weather endpoints
        weatherController.registerRoutes(app);

        // Simpelt endpoint til at kontrollere at API'et kører
        app.get("/", ctx ->
                ctx.result("Travel Planner API is running!")
        );

        System.out.println(
                "Travel Planner started on http://localhost:7070"
        );
    }
}