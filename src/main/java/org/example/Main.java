package org.example;

import io.javalin.Javalin;
import org.example.config.HibernateConfig;
import org.example.controller.AuthController;
import org.example.controller.DestinationController;
import org.example.controller.WeatherController;

public class Main {

    public static void main(String[] args) {

        // Forbinder til databasen.
        HibernateConfig.getEntityManagerFactory();

        // Starter serveren.
        Javalin app = Javalin.create().start(7070);

        // Weather endpoints.
        WeatherController weatherController = new WeatherController();
        weatherController.registerRoutes(app);

        // Register, login og logout.
        AuthController authController = new AuthController();
        authController.registerRoutes(app);

        // Destinationer, rejsekladde og flysøgning.
        DestinationController destinationController =
                new DestinationController();

        destinationController.registerRoutes(app);

        app.get("/", ctx ->
                ctx.result("Travel Planner API is running!")
        );

        System.out.println(
                "Travel Planner started on http://localhost:7070"
        );
    }
}