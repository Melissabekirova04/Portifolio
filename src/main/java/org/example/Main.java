package org.example;

import com.fasterxml.jackson.core.exc.StreamReadException;
import io.javalin.Javalin;
import io.javalin.http.BadGatewayResponse;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpResponseException;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.ServiceUnavailableResponse;
import io.javalin.http.UnauthorizedResponse;
import org.example.config.HibernateConfig;
import org.example.controller.ActivityController;
import org.example.controller.AuthController;
import org.example.controller.DestinationController;
import org.example.controller.DestinationInspirationController;
import org.example.controller.ExperienceController;
import org.example.controller.FlightSelectionController;
import org.example.controller.HotelController;
import org.example.controller.ItineraryController;
import org.example.controller.TripController;
import org.example.controller.WeatherController;
import org.example.security.JwtService;
import org.example.security.SecurityMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Main {

    private static final Logger logger =
            LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        JwtService jwtService = new JwtService();

        HibernateConfig.getEntityManagerFactory();

        Javalin app = Javalin.create();

        registerExceptionHandlers(app);

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

        ExperienceController experienceController =
                new ExperienceController();
        experienceController.registerRoutes(app);

        FlightSelectionController flightSelectionController =
                new FlightSelectionController();
        flightSelectionController.registerRoutes(app);

        HotelController hotelController =
                new HotelController();
        hotelController.registerRoutes(app);

        TripController tripController = new TripController();
        tripController.registerRoutes(app);

        ActivityController activityController =
                new ActivityController();
        activityController.registerRoutes(app);

        ItineraryController itineraryController =
                new ItineraryController();
        itineraryController.registerRoutes(app);

        app.get("/", ctx ->
                ctx.result("Travel Planner API is running!")
        );

        // Log metode, sti og statuskode.
        app.after(ctx ->
                logger.info(
                        "{} {} -> {}",
                        ctx.req().getMethod(),
                        ctx.path(),
                        ctx.statusCode()
                )
        );

        app.start(7070);

        logger.info(
                "Travel Planner started on http://localhost:7070"
        );
    }

    private static void registerExceptionHandlers(Javalin app) {

        registerHttpError(app, BadRequestResponse.class, 400);
        registerHttpError(app, UnauthorizedResponse.class, 401);
        registerHttpError(app, ForbiddenResponse.class, 403);
        registerHttpError(app, NotFoundResponse.class, 404);
        registerHttpError(app, ConflictResponse.class, 409);
        registerHttpError(app, BadGatewayResponse.class, 502);
        registerHttpError(app, ServiceUnavailableResponse.class, 503);

        // Ugyldigt JSON i en request.
        app.exception(StreamReadException.class, (exception, ctx) -> {
            ctx.status(400).json(Map.of(
                    "status", 400,
                    "error", "Ugyldigt JSON-format. Kontrollér dit input."
            ));
        });

        // Uventede fejl logges med stacktrace.
        app.exception(Exception.class, (exception, ctx) -> {
            logger.error(
                    "Uventet fejl ved {} {}",
                    ctx.req().getMethod(),
                    ctx.path(),
                    exception
            );

            ctx.status(500).json(Map.of(
                    "status", 500,
                    "error", "Der opstod en intern serverfejl."
            ));
        });
    }

    private static <T extends HttpResponseException>
    void registerHttpError(
            Javalin app,
            Class<T> exceptionType,
            int status
    ) {
        app.exception(exceptionType, (exception, ctx) -> {
            String message = exception.getMessage();

            if (message == null || message.isBlank()) {
                message = "Kaldet kunne ikke gennemføres.";
            }

            if (status >= 500) {
                logger.warn(
                        "Tjenestefejl ved {} {}: HTTP {}",
                        ctx.req().getMethod(),
                        ctx.path(),
                        status
                );
            }

            ctx.status(status).json(Map.of(
                    "status", status,
                    "error", message
            ));
        });
    }
}