package org.example;

import io.javalin.Javalin;
import org.example.config.HibernateConfig;
import org.example.controller.WeatherController;

public class Main {

    public static void main(String[] args) {

        // Opretter forbindelsen til PostgreSQL og registrerer User-tabellen
        HibernateConfig.getEntityManagerFactory();

        // Starter Javalin-serveren
        Javalin app = Javalin.create().start(7070);

        // Beholder dine eksisterende weather endpoints
        WeatherController weatherController = new WeatherController();
        weatherController.registerRoutes(app);

        // Kontrollerer, at API'et kører
        app.get("/", ctx ->
                ctx.result("Travel Planner API is running!")
        );

        System.out.println(
                "Travel Planner started on http://localhost:7070"
        );
    }
}