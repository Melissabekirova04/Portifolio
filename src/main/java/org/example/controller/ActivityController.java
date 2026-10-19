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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ActivityController {

    private final TripDAO tripDAO = new TripDAO();

    private final ExperienceService experienceService =
            new ExperienceService();

    private final ObjectMapper mapper = new ObjectMapper();

    // US12: Vælg en anbefalet oplevelse.
    public record ActivityChoice(
            String experienceId,
            String date,
            String time
    ) {}

    // US13: Opret eller rediger en aktivitet.
    // priceAmount er valgfri. null betyder ukendt pris.
    public record ActivityInput(
            String name,
            String date,
            String time,
            BigDecimal priceAmount,
            String category,
            String notes
    ) {}

    private record ValidatedInput(
            String name,
            LocalDate date,
            LocalTime time,
            BigDecimal priceAmount,
            String category,
            String notes
    ) {}

    public void registerRoutes(Javalin app) {

        // US12: Gem en anbefalet oplevelse på rejsen.
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

            activity.put("experienceId", experience.id());
            activity.put("id", UUID.randomUUID().toString());
            activity.put("type", "recommended");
            activity.put("date", date.toString());
            activity.put("time", time.toString());
            activity.put("timeBasis", "Lokal tid på destinationen");
            activity.put("notes", "");
            activity.put("addedAt", Instant.now().toString());

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> {
                        String destinationCode = readJson(
                                trip.getDestinationJson()
                        ).path("airportCode").asText();

                        if (!experience.destination()
                                .equals(destinationCode)) {
                            throw new BadRequestResponse(
                                    "Oplevelsen hører ikke til "
                                            + "rejsens destination."
                            );
                        }

                        validateDateWithinTrip(trip, date);

                        ArrayNode activities = readActivities(trip);
                        activities.add(activity);

                        trip.updateActivities(toJson(activities));
                    }
            ).orElseThrow(this::tripNotFound);

            ctx.status(201).json(
                    activityResponse(
                            updated.getId(),
                            activity,
                            "Oplevelsen er tilføjet til rejsen."
                    )
            );
        });

        // US13: Opret en egen aktivitet.
        app.post("/api/trips/{id}/activities/custom", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            requireTrip(tripId, userId);

            ValidatedInput input = validateInput(
                    ctx.bodyAsClass(ActivityInput.class)
            );

            ObjectNode activity = mapper.createObjectNode();

            activity.put("id", UUID.randomUUID().toString());
            activity.put("type", "custom");
            activity.put("addedAt", Instant.now().toString());

            applyInput(activity, input);

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> {
                        validateDateWithinTrip(trip, input.date());

                        activity.put(
                                "destination",
                                readJson(trip.getDestinationJson())
                                        .path("airportCode").asText()
                        );

                        ArrayNode activities = readActivities(trip);
                        activities.add(activity);

                        trip.updateActivities(toJson(activities));
                    }
            ).orElseThrow(this::tripNotFound);

            ctx.status(201).json(
                    activityResponse(
                            updated.getId(),
                            activity,
                            "Din aktivitet er oprettet."
                    )
            );
        });

        // Hent alle aktiviteter på rejsen.
        app.get("/api/trips/{id}/activities", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip trip = requireTrip(tripId, userId);
            ArrayNode activities = readActivities(trip);

            List<JsonNode> sorted = new ArrayList<>();
            activities.forEach(sorted::add);

            sorted.sort(
                    Comparator
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
            response.set("activities", mapper.valueToTree(sorted));

            ctx.json(response);
        });

        // US13: Rediger en aktivitet.
        // Send alle felterne i ActivityInput ved PUT.
        app.put("/api/trips/{id}/activities/{activityId}", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));
            String activityId = ctx.pathParam("activityId");

            requireTrip(tripId, userId);

            ValidatedInput input = validateInput(
                    ctx.bodyAsClass(ActivityInput.class)
            );

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> {
                        ArrayNode activities = readActivities(trip);
                        int index = findActivityIndex(
                                activities, activityId
                        );

                        validateDateWithinTrip(trip, input.date());

                        // Behold ID, kilde og øvrige oplysninger.
                        ObjectNode activity =
                                ((ObjectNode) activities.get(index))
                                        .deepCopy();

                        applyInput(activity, input);
                        activity.put(
                                "updatedAt",
                                Instant.now().toString()
                        );

                        activities.set(index, activity);
                        trip.updateActivities(toJson(activities));
                    }
            ).orElseThrow(this::tripNotFound);

            ArrayNode activities = readActivities(updated);

            JsonNode activity = activities.get(
                    findActivityIndex(activities, activityId)
            );

            ctx.json(
                    activityResponse(
                            tripId,
                            activity,
                            "Aktiviteten er opdateret."
                    )
            );
        });

        // US13: Slet en aktivitet fra rejsen.
        app.delete("/api/trips/{id}/activities/{activityId}", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));
            String activityId = ctx.pathParam("activityId");

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> {
                        ArrayNode activities = readActivities(trip);
                        int index = findActivityIndex(
                                activities, activityId
                        );

                        activities.remove(index);
                        trip.updateActivities(toJson(activities));
                    }
            ).orElseThrow(this::tripNotFound);

            ObjectNode response = mapper.createObjectNode();

            response.put("tripId", updated.getId());
            response.put("deletedActivityId", activityId);
            response.put("message", "Aktiviteten er slettet.");

            ctx.json(response);
        });
    }

    private ValidatedInput validateInput(ActivityInput input) {
        String name = requiredText(input.name(), "Navn", 200);

        LocalDate date = parseDate(input.date());
        LocalTime time = parseTime(input.time());

        String category = optionalText(
                input.category(), "Kategori", 100
        );

        String notes = optionalText(
                input.notes(), "Noter", 5000
        );

        BigDecimal price = input.priceAmount();

        if (price != null) {
            if (price.signum() < 0) {
                throw new BadRequestResponse(
                        "Prisen må ikke være negativ."
                );
            }

            try {
                price = price.setScale(2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException e) {
                throw new BadRequestResponse(
                        "Prisen må højst have to decimaler."
                );
            }

            if (price.compareTo(
                    new BigDecimal("1000000000.00")
            ) > 0) {
                throw new BadRequestResponse(
                        "Prisen er for høj."
                );
            }
        }

        return new ValidatedInput(
                name, date, time, price, category, notes
        );
    }

    private void applyInput(
            ObjectNode activity,
            ValidatedInput input
    ) {
        activity.put("name", input.name());
        activity.put("date", input.date().toString());
        activity.put("time", input.time().toString());
        activity.put("timeBasis", "Lokal tid på destinationen");
        activity.put("category", input.category());
        activity.put("notes", input.notes());

        if (input.priceAmount() == null) {
            activity.putNull("priceAmount");
            activity.putNull("currency");
        } else {
            activity.put("priceAmount", input.priceAmount());
            activity.put("currency", "DKK");
        }

        activity.put(
                "priceNotice",
                "Brugerens egen planlægningspris. "
                        + "null betyder ukendt pris; 0 betyder gratis. "
                        + "Prisen indgår endnu ikke i fly- og hoteltotalen."
        );
    }

    private String requiredText(
            String value,
            String label,
            int maxLength
    ) {
        String text = optionalText(value, label, maxLength);

        if (text.isBlank()) {
            throw new BadRequestResponse(
                    label + " skal udfyldes."
            );
        }

        return text;
    }

    private String optionalText(
            String value,
            String label,
            int maxLength
    ) {
        String text = value == null ? "" : value.trim();

        if (text.length() > maxLength) {
            throw new BadRequestResponse(
                    label + " må højst indeholde "
                            + maxLength + " tegn."
            );
        }

        return text;
    }

    private int findActivityIndex(
            ArrayNode activities,
            String activityId
    ) {
        for (int index = 0; index < activities.size(); index++) {
            if (activityId.equals(
                    activities.get(index).path("id").asText()
            )) {
                return index;
            }
        }

        throw new NotFoundResponse(
                "Aktiviteten findes ikke på denne rejse."
        );
    }

    private void validateDateWithinTrip(
            Trip trip,
            LocalDate date
    ) {
        JsonNode search = readJson(trip.getSearchJson());

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

        if (activities.isNull()) {
            return mapper.createArrayNode();
        }

        if (!activities.isArray()) {
            throw new IllegalStateException(
                    "De gemte aktiviteter har et ugyldigt format."
            );
        }

        for (JsonNode activity : activities) {
            if (!activity.isObject()) {
                throw new IllegalStateException(
                        "En gemt aktivitet har et ugyldigt format."
                );
            }
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

    private ObjectNode activityResponse(
            Long tripId,
            JsonNode activity,
            String message
    ) {
        ObjectNode response = mapper.createObjectNode();

        response.put("tripId", tripId);
        response.put("message", message);
        response.set("activity", activity);

        return response;
    }
}