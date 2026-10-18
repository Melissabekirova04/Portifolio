package org.example;

import io.javalin.Javalin;
import org.example.config.HibernateConfig;
import org.example.controller.AuthController;
import org.example.controller.DestinationController;
import org.example.controller.DestinationInspirationController;
import org.example.controller.FlightSelectionController;
import org.example.controller.HotelController;
import org.example.controller.TripController;
import org.example.controller.WeatherController;
import org.example.security.JwtService;
import org.example.security.SecurityMiddleware;

public class Main {

    public static void main(String[] args) {

        JwtService jwtService = new JwtService();

        HibernateConfig.getEntityManagerFactory();

        Javalin app = Javalin.create();

        SecurityMiddleware security =
                new SecurityMiddleware(jwtService);
        security.register(app);

        WeatherController weatherController =
                new WeatherController();
        weatherController.registerRoutes(app);

        AuthController authController =
                new AuthController(jwtService);
        authController.registerRoutes(app);

        DestinationController destinationController =
                new DestinationController();
        destinationController.registerRoutes(app);

        DestinationInspirationController inspirationController =
                new DestinationInspirationController();
        inspirationController.registerRoutes(app);

        FlightSelectionController flightSelectionController =
                new FlightSelectionController();
        flightSelectionController.registerRoutes(app);

        HotelController hotelController =
                new HotelController();
        hotelController.registerRoutes(app);

        TripController tripController = new TripController();
        tripController.registerRoutes(app);

        app.get("/", ctx ->
                ctx.result("Travel Planner API is running!")
        );

        app.start(7070);

        System.out.println(
                "Travel Planner started on http://localhost:7070"
        );
    }
}