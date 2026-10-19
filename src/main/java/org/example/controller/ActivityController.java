package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import org.example.dao.TripDAO;
import org.example.entity.Trip;
import org.example.security.SecurityMiddleware;
import org.example.service.ExperienceService;
import org.example.service.ExperienceService.Experience;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public class ActivityController {

    private final TripDAO tripDAO = new TripDAO();

    private final ExperienceService experienceService =
            new ExperienceService();

    private final ObjectMapper mapper = new ObjectMapper();

    public record ActivityChoice(
            String experienceId,
            String date,
            String time
    ) {}

    public void registerRoutes(Javalin app) {

        // US12: Tilføj en oplevelse med dato og tidspunkt.
        app.post("/api/trips/{id}/activities", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            requireTrip(tripId, userId);

            ActivityChoice input =
                    ctx.bodyAsClass(ActivityChoice.class);

            if (input.experienceId() == null
                    || input.experienceId().isBlank()) {
                throw new BadRequestResponse(
                        "experienceId mangler."
                );
            }

            Experience experience = experienceService
                    .findById(input.experienceId())
                    .orElseThrow(() -> new NotFoundResponse(
                            "Oplevelsen findes ikke."
                    ));

            LocalDate date = parseDate(input.date());
            LocalTime time = parseTime(input.time());

            ObjectNode activity = mapper.valueToTree(experience);

            // Aktiviteten får sit eget ID.
            activity.put("experienceId", experience.id());
            activity.put("id", UUID.randomUUID().toString());
            activity.put("date", date.toString());
            activity.put("time", time.toString());
            activity.put("timeBasis", "Lokal tid på destinationen");
            activity.put("addedAt", Instant.now().toString());

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> {
                        validateActivity(trip, experience, date);

                        ArrayNode activities = readActivities(trip);
                        activities.add(activity);

                        trip.updateActivities(toJson(activities));
                    }
            ).orElseThrow(this::tripNotFound);

            ObjectNode response = mapper.createObjectNode();

            response.put("tripId", updated.getId());
            response.put("message", "Oplevelsen er tilføjet til rejsen.");
            response.set("activity", activity);

            ctx.status(201).json(response);
        });

        // Hent rejseplanens aktiviteter fra databasen.
        app.get("/api/trips/{id}/activities", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip trip = requireTrip(tripId, userId);
            ArrayNode activities = readActivities(trip);

            // Vis aktiviteter i rækkefølge efter dato og tidspunkt.
            java.util.List<JsonNode> sorted =
                    new java.util.ArrayList<>();

            activities.forEach(sorted::add);

            sorted.sort(
                    java.util.Comparator
                            .comparing((JsonNode activity) ->
                                    activity.path("date").asText())
                            .thenComparing(activity ->
                                    activity.path("time").asText())
                            .thenComparing(activity ->
                                    activity.path("id").asText())
            );

            ObjectNode response = mapper.createObjectNode();

            response.put("tripId", tripId);
            response.put("count", sorted.size());
            response.set(
                    "activities",
                    mapper.valueToTree(sorted)
            );

            ctx.json(response);
        });
    }

    private void validateActivity(
            Trip trip,
            Experience experience,
            LocalDate date
    ) {
        JsonNode destination = readJson(trip.getDestinationJson());
        JsonNode search = readJson(trip.getSearchJson());

        String destinationCode =
                destination.path("airportCode").asText();

        if (!experience.destination().equals(destinationCode)) {
            throw new BadRequestResponse(
                    "Oplevelsen hører ikke til rejsens destination."
            );
        }

        LocalDate startDate;
        LocalDate endDate;

        try {
            startDate = LocalDate.parse(
                    search.path("departureDate").asText()
            );

            endDate = LocalDate.parse(
                    search.path("returnDate").asText()
            );
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    "Rejsen mangler gyldige start- og slutdatoer."
            );
        }

        if (!endDate.isAfter(startDate)) {
            throw new BadRequestResponse(
                    "Rejsens slutdato skal ligge efter startdatoen."
            );
        }

        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            throw new BadRequestResponse(
                    "Aktivitetens dato skal ligge mellem "
                            + startDate + " og " + endDate + "."
            );
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null
                || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BadRequestResponse(
                    "Angiv dato som YYYY-MM-DD."
            );
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    "Aktivitetens dato er ugyldig."
            );
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || !value.matches("\\d{2}:\\d{2}")) {
            throw new BadRequestResponse(
                    "Angiv tidspunkt som HH:mm, fx 10:30."
            );
        }

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    "Aktivitetens tidspunkt er ugyldigt."
            );
        }
    }

    private Trip requireTrip(Long tripId, Long userId) {
        return tripDAO.findByIdAndUserId(tripId, userId)
                .orElseThrow(this::tripNotFound);
    }

    private NotFoundResponse tripNotFound() {
        return new NotFoundResponse("Rejsen findes ikke.");
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

    private ArrayNode readActivities(Trip trip) {
        JsonNode activities = readJson(trip.getActivitiesJson());

        // Ældre rejser har endnu ingen aktiviteter.
        if (activities.isNull()) {
            return mapper.createArrayNode();
        }

        if (!activities.isArray()) {
            throw new IllegalStateException(
                    "De gemte aktiviteter har et ugyldigt format."
            );
        }

        return (ArrayNode) activities;
    }

    private JsonNode readJson(String value) {
        if (value == null) {
            return mapper.nullNode();
        }

        try {
            return mapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke læse rejseoplysninger.", e
            );
        }
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke gemme aktivitetens oplysninger.", e
            );
        }
    }
}