package org.example;

import io.javalin.Javalin;
import org.example.config.HibernateConfig;
import org.example.controller.AuthController;
import org.example.controller.DestinationController;
import org.example.controller.TripController;
import org.example.controller.WeatherController;

public class Main {

    public static void main(String[] args) {

        // Forbinder til databasen
        HibernateConfig.getEntityManagerFactory();

        // Starter serveren
        Javalin app = Javalin.create().start(7070);

        // Registrerer weather endpoints
        WeatherController weatherController = new WeatherController();
        weatherController.registerRoutes(app);

        // Registrerer opret konto, login og logout
        AuthController authController = new AuthController();
        authController.registerRoutes(app);

        // Registrerer destinationssøgning og rejsekladde
        DestinationController destinationController =
                new DestinationController();
        destinationController.registerRoutes(app);

        // Registrerer gem og hent rejser fra databasen
        TripController tripController = new TripController();
        tripController.registerRoutes(app);

        app.get("/", ctx ->
                ctx.result("Travel Planner API is running!")
        );

        System.out.println(
                "Travel Planner started on http://localhost:7070"
        );
    }
}