package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.example.dao.TripDAO;
import org.example.entity.Trip;

import java.util.ArrayList;
import java.util.List;

public class TripController {

    private final TripDAO tripDAO = new TripDAO();
    private final ObjectMapper mapper = new ObjectMapper();

    public void registerRoutes(Javalin app) {

        // Gem den aktuelle rejsekladde i databasen.
        app.post("/api/trips", ctx -> {
            Long userId = requireUserId(ctx);

            Object destination =
                    ctx.sessionAttribute("tripDestination");

            if (destination == null) {
                throw new BadRequestResponse(
                        "Du skal vælge en destination først"
                );
            }

            Object search = ctx.sessionAttribute("flightSearch");
            Object flight = ctx.sessionAttribute("selectedFlight");

            Trip trip = tripDAO.create(
                    userId,
                    mapper.writeValueAsString(destination),
                    toJsonOrNull(search),
                    toJsonOrNull(flight)
            );

            // Kladden ryddes, når rejsen er gemt.
            ctx.sessionAttribute("tripDestination", null);
            ctx.sessionAttribute("flightSearch", null);
            ctx.sessionAttribute("flightOffers", null);
            ctx.sessionAttribute("selectedFlight", null);

            ctx.status(201).json(toResponse(trip));
        });

        // Hent alle rejser for den indloggede bruger.
        app.get("/api/trips", ctx -> {
            Long userId = requireUserId(ctx);

            List<ObjectNode> response = new ArrayList<>();

            for (Trip trip : tripDAO.findByUserId(userId)) {
                response.add(toResponse(trip));
            }

            ctx.json(response);
        });

        // Hent én rejse, hvis den tilhører brugeren.
        app.get("/api/trips/{id}", ctx -> {
            Long userId = requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip trip = tripDAO.findByIdAndUserId(tripId, userId)
                    .orElseThrow(() -> new NotFoundResponse(
                            "Rejsen findes ikke"
                    ));

            ctx.json(toResponse(trip));
        });
    }

    private Long requireUserId(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedResponse(
                    "Du skal logge ind først"
            );
        }

        return userId;
    }

    private Long parseTripId(String value) {
        try {
            long id = Long.parseLong(value);

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;
        } catch (NumberFormatException e) {
            throw new BadRequestResponse(
                    "Rejsens ID skal være et positivt heltal"
            );
        }
    }

    private String toJsonOrNull(Object value)
            throws JsonProcessingException {
        return value == null
                ? null
                : mapper.writeValueAsString(value);
    }

    private JsonNode readJsonOrNull(String value)
            throws JsonProcessingException {
        return value == null
                ? mapper.nullNode()
                : mapper.readTree(value);
    }

    private ObjectNode toResponse(Trip trip)
            throws JsonProcessingException {
        ObjectNode response = mapper.createObjectNode();

        response.put("id", trip.getId());
        response.put("createdAt", trip.getCreatedAt().toString());

        response.set(
                "destination",
                mapper.readTree(trip.getDestinationJson())
        );

        response.set(
                "search",
                readJsonOrNull(trip.getSearchJson())
        );

        response.set(
                "flight",
                readJsonOrNull(trip.getFlightJson())
        );

        return response;
    }
}