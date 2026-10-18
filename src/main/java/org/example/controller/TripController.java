package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import org.example.dao.TripDAO;
import org.example.entity.Trip;
import org.example.security.SecurityMiddleware;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TripController {

    private final TripDAO tripDAO = new TripDAO();
    private final ObjectMapper mapper = new ObjectMapper();

    public void registerRoutes(Javalin app) {

        // Gem rejsekladden på den bruger, som JWT-tokenet tilhører.
        app.post("/api/trips", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);

            Object destination =
                    ctx.sessionAttribute("tripDestination");

            if (destination == null) {
                throw new BadRequestResponse(
                        "Du skal vælge en destination først."
                );
            }

            Object search = ctx.sessionAttribute("flightSearch");
            Object flight = ctx.sessionAttribute("selectedFlight");

            validateDates(search);

            Trip trip = tripDAO.create(
                    userId,
                    mapper.writeValueAsString(destination),
                    toJsonOrNull(search),
                    toJsonOrNull(flight)
            );

            // Ryd først kladden, når rejsen er gemt.
            ctx.sessionAttribute("tripDestination", null);
            ctx.sessionAttribute("flightSearch", null);
            ctx.sessionAttribute("flightOffers", null);
            ctx.sessionAttribute("selectedFlight", null);

            ctx.status(201).json(toResponse(trip));
        });

        // Hent den indloggede brugers rejser.
        app.get("/api/trips", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);

            List<ObjectNode> response = new ArrayList<>();

            for (Trip trip : tripDAO.findByUserId(userId)) {
                response.add(toResponse(trip));
            }

            ctx.json(response);
        });

        // Hent én rejse, hvis den tilhører brugeren.
        app.get("/api/trips/{id}", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip trip = tripDAO.findByIdAndUserId(tripId, userId)
                    .orElseThrow(() -> new NotFoundResponse(
                            "Rejsen findes ikke."
                    ));

            ctx.json(toResponse(trip));
        });
    }

    private void validateDates(Object search) {
        if (search == null) {
            throw new BadRequestResponse(
                    "Du skal vælge startdato og slutdato, "
                            + "før rejsen gemmes."
            );
        }

        JsonNode searchData = mapper.valueToTree(search);

        LocalDate startDate;
        LocalDate endDate;

        try {
            startDate = LocalDate.parse(
                    searchData.path("departureDate").asText()
            );

            endDate = LocalDate.parse(
                    searchData.path("returnDate").asText()
            );

        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    "Rejsen skal have gyldige start- og slutdatoer."
            );
        }

        if (!endDate.isAfter(startDate)) {
            throw new BadRequestResponse(
                    "Slutdatoen skal ligge efter startdatoen."
            );
        }

        if (startDate.isBefore(
                LocalDate.now(ZoneId.of("Europe/Copenhagen"))
        )) {
            throw new BadRequestResponse(
                    "Startdatoen må ikke være i fortiden."
            );
        }
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
                    "Rejsens ID skal være et positivt heltal."
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