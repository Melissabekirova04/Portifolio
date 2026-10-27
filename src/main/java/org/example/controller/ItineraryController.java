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
import org.example.dto.ActivityDTO;
import org.example.entity.Trip;
import org.example.security.SecurityMiddleware;
import org.example.service.ActivityService;
import org.example.service.BudgetService;
import org.example.service.BudgetService.BudgetResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ItineraryController {

    private static final BigDecimal TEST_EUR_TO_DKK =
            new BigDecimal("7.50");

    private final TripDAO tripDAO = new TripDAO();

    private final ActivityService activityService =
            new ActivityService();

    private final BudgetService budgetService =
            new BudgetService();

    private final ObjectMapper mapper = new ObjectMapper();

    public record BudgetInput(BigDecimal budgetAmount) {}

    public void registerRoutes(Javalin app) {

        // Hent samlet rejseplan og budget.
        app.get("/api/trips/{id}/itinerary", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip trip = requireTrip(tripId, userId);

            List<ActivityDTO> activities =
                    findActivities(tripId, userId);

            ctx.json(buildItinerary(trip, activities));
        });

        // Opret eller opdater budgetgrænsen.
        app.put("/api/trips/{id}/budget", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            requireTrip(tripId, userId);

            BudgetInput input =
                    ctx.bodyAsClass(BudgetInput.class);

            if (input == null) {
                throw new BadRequestResponse(
                        "Request body mangler."
                );
            }

            BigDecimal amount = validateBudget(
                    input.budgetAmount()
            );

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> trip.updateBudget(amount)
            ).orElseThrow(() ->
                    new NotFoundResponse("Rejsen findes ikke.")
            );

            List<ActivityDTO> activities =
                    findActivities(tripId, userId);

            ObjectNode response = mapper.createObjectNode();

            response.put("tripId", tripId);
            response.put("message", "Budgettet er gemt.");
            response.set(
                    "budget",
                    calculateBudget(updated, activities)
            );

            ctx.json(response);
        });

        // Fjern budgetgrænsen.
        app.delete("/api/trips/{id}/budget", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> trip.updateBudget(null)
            ).orElseThrow(() ->
                    new NotFoundResponse("Rejsen findes ikke.")
            );

            List<ActivityDTO> activities =
                    findActivities(tripId, userId);

            ObjectNode response = mapper.createObjectNode();

            response.put("tripId", tripId);
            response.put("message", "Budgetgrænsen er fjernet.");
            response.set(
                    "budget",
                    calculateBudget(updated, activities)
            );

            ctx.json(response);
        });
    }

    private List<ActivityDTO> findActivities(
            Long tripId,
            Long userId
    ) {
        try {
            return activityService.findAll(tripId, userId);
        } catch (NoSuchElementException e) {
            throw new NotFoundResponse(e.getMessage());
        }
    }

    private ObjectNode buildItinerary(
            Trip trip,
            List<ActivityDTO> activities
    ) {
        JsonNode search = readJson(trip.getSearchJson());
        JsonNode flight = readJson(trip.getFlightJson());
        JsonNode hotel = readJson(trip.getHotelJson());

        LocalDate start = parseDate(
                search.path("departureDate").asText()
        );

        LocalDate end = parseDate(
                search.path("returnDate").asText()
        );

        if (!end.isAfter(start)) {
            throw new BadRequestResponse(
                    "Slutdatoen skal ligge efter startdatoen."
            );
        }

        LocalDate checkIn = null;
        LocalDate checkOut = null;

        if (!hotel.isNull()) {
            checkIn = parseDate(
                    hotel.path("checkIn").asText()
            );
            checkOut = parseDate(
                    hotel.path("checkOut").asText()
            );
        }

        ArrayNode days = mapper.createArrayNode();

        for (LocalDate date = start;
             !date.isAfter(end);
             date = date.plusDays(1)) {

            ObjectNode day = mapper.createObjectNode();
            day.put("date", date.toString());

            ArrayNode dailyActivities = mapper.createArrayNode();

            for (ActivityDTO activity : activities) {
                if (date.toString().equals(activity.date())) {
                    JsonNode activityJson =
                            mapper.valueToTree(activity);

                    dailyActivities.add(activityJson);
                }
            }

            day.set("activities", dailyActivities);
            day.set(
                    "flights",
                    flightsDepartingOn(flight, date.toString())
            );

            boolean hotelNight = false;

            if (checkIn != null && checkOut != null) {
                hotelNight = !date.isBefore(checkIn)
                        && date.isBefore(checkOut);
            }

            day.put("hotelNight", hotelNight);

            if (hotelNight) {
                day.put(
                        "hotelName",
                        hotel.path("name").asText()
                );
                day.put(
                        "hotelId",
                        hotel.path("id").asText()
                );
            }

            days.add(day);
        }

        ObjectNode response = mapper.createObjectNode();

        response.put("tripId", trip.getId());
        response.set(
                "destination",
                readJson(trip.getDestinationJson())
        );
        response.put("startDate", start.toString());
        response.put("endDate", end.toString());
        response.set("flight", flight);
        response.set("hotel", hotel);
        response.set(
                "activities",
                mapper.valueToTree(activities)
        );
        response.set("days", days);
        response.set(
                "budget",
                calculateBudget(trip, activities)
        );

        response.put(
                "timeNotice",
                "Aktivitetstider er lokal tid på destinationen. "
                        + "Flytider er lokale tider for de respektive "
                        + "lufthavne. Fly vises på afrejsedagen."
        );

        return response;
    }

    private ArrayNode flightsDepartingOn(
            JsonNode flight,
            String date
    ) {
        ArrayNode result = mapper.createArrayNode();

        for (JsonNode slice : flight.path("slices")) {
            for (JsonNode segment : slice.path("segments")) {

                String departure =
                        segment.path("departing_at").asText();

                if (departure.length() < 10
                        || !departure.substring(0, 10).equals(date)) {
                    continue;
                }

                ObjectNode item = mapper.createObjectNode();

                item.put(
                        "segmentId",
                        segment.path("id").asText()
                );
                item.put("departureTime", departure);
                item.put(
                        "arrivalTime",
                        segment.path("arriving_at").asText()
                );
                item.put(
                        "origin",
                        segment.path("origin")
                                .path("iata_code").asText()
                );
                item.put(
                        "destination",
                        segment.path("destination")
                                .path("iata_code").asText()
                );
                item.put(
                        "airline",
                        segment.path("marketing_carrier")
                                .path("name").asText()
                );
                item.put(
                        "flightNumber",
                        segment.path("marketing_carrier")
                                .path("iata_code").asText()
                                + segment.path(
                                "marketing_carrier_flight_number"
                        ).asText()
                );
                item.put(
                        "duration",
                        segment.path("duration").asText()
                );

                result.add(item);
            }
        }

        return result;
    }

    private ObjectNode calculateBudget(
            Trip trip,
            List<ActivityDTO> activities
    ) {
        JsonNode flight = readJson(trip.getFlightJson());
        JsonNode hotel = readJson(trip.getHotelJson());

        // Fravalgt fly eller hotel tæller som 0 kr.
        // Et valgt tilbud med ukendt pris giver null.
        BigDecimal flightAmount = BigDecimal.ZERO;
        BigDecimal hotelAmount = BigDecimal.ZERO;

        BigDecimal knownActivityAmount = BigDecimal.ZERO;
        List<BigDecimal> activityPrices = new ArrayList<>();

        boolean estimatedConversion = false;
        int unknownActivityCount = 0;

        ArrayNode unknownPrices = mapper.createArrayNode();

        if (!flight.isNull()) {
            String currency =
                    flight.path("total_currency").asText();

            flightAmount = amountInDkk(
                    readAmount(flight.path("total_amount")),
                    currency
            );

            if (flightAmount == null) {
                unknownPrices.add(
                        "Flypris mangler eller valuta understøttes ikke."
                );
            } else if ("EUR".equals(currency)) {
                estimatedConversion = true;
            }
        }

        if (!hotel.isNull()) {
            String currency = hotel.path("currency").asText();

            hotelAmount = amountInDkk(
                    readAmount(hotel.path("totalAmount")),
                    currency
            );

            if (hotelAmount == null) {
                unknownPrices.add(
                        "Hotelpris mangler eller valuta understøttes ikke."
                );
            } else if ("EUR".equals(currency)) {
                estimatedConversion = true;
            }
        }

        for (ActivityDTO activity : activities) {
            BigDecimal converted = amountInDkk(
                    activity.priceAmount(),
                    activity.currency()
            );

            // Listen beholder null for ukendte priser.
            activityPrices.add(converted);

            if (converted == null) {
                unknownActivityCount++;

                unknownPrices.add(
                        "Aktivitet: " + activity.name()
                );
            } else {
                knownActivityAmount =
                        knownActivityAmount.add(converted);

                if ("EUR".equals(activity.currency())) {
                    estimatedConversion = true;
                }
            }
        }

        BigDecimal limit = trip.getBudgetAmount();

        // Brug den beregning, som vores unit tests tester.
        BudgetResult result = budgetService.calculate(
                limit,
                flightAmount,
                hotelAmount,
                activityPrices
        );

        ObjectNode budget = mapper.createObjectNode();

        budget.put("currency", "DKK");
        budget.set(
                "budgetAmount",
                mapper.valueToTree(limit)
        );

        if (flightAmount == null) {
            budget.putNull("flightAmount");
        } else {
            budget.put("flightAmount", money(flightAmount));
        }

        if (hotelAmount == null) {
            budget.putNull("hotelAmount");
        } else {
            budget.put("hotelAmount", money(hotelAmount));
        }

        budget.put(
                "knownActivitiesAmount",
                money(knownActivityAmount)
        );

        budget.put(
                "knownTotalAmount",
                result.knownTotalAmount()
        );

        budget.put(
                "priceComplete",
                result.priceComplete()
        );

        budget.put(
                "unknownActivityPriceCount",
                unknownActivityCount
        );

        budget.set("unknownPrices", unknownPrices);

        budget.put(
                "estimatedConversion",
                estimatedConversion
        );

        budget.set(
                "totalAmount",
                mapper.valueToTree(result.totalAmount())
        );

        budget.set(
                "remainingBudget",
                mapper.valueToTree(result.remainingBudget())
        );

        budget.set(
                "overBudget",
                mapper.valueToTree(result.overBudget())
        );

        if (limit != null) {
            budget.put(
                    "remainingAfterKnownCosts",
                    money(limit.subtract(result.knownTotalAmount()))
            );

            budget.put(
                    "knownCostsExceedBudget",
                    result.knownTotalAmount().compareTo(limit) > 0
            );
        } else {
            budget.putNull("remainingAfterKnownCosts");
            budget.putNull("knownCostsExceedBudget");
        }

        budget.put(
                "priceNotice",
                "Planlægningspriser, ingen reservationer. "
                        + "Hotelpriser er fiktive. EUR omregnes med "
                        + "fast testkurs på 7,50 DKK. "
                        + "Ukendte priser gør totalen ufuldstændig. "
                        + "Ikke-valgte fly og hoteller indgår med 0 kr."
        );

        return budget;
    }

    private BigDecimal readAmount(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }

        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal amountInDkk(
            BigDecimal amount,
            String currency
    ) {
        if (amount == null || amount.signum() < 0) {
            return null;
        }

        if ("DKK".equals(currency)) {
            return money(amount);
        }

        if ("EUR".equals(currency)) {
            return money(amount.multiply(TEST_EUR_TO_DKK));
        }

        return null;
    }

    private BigDecimal validateBudget(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestResponse(
                    "Budgettet skal være et positivt beløb."
            );
        }

        if (amount.compareTo(
                new BigDecimal("9999999999.99")
        ) > 0) {
            throw new BadRequestResponse(
                    "Budgettet er for højt."
            );
        }

        try {
            return amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException e) {
            throw new BadRequestResponse(
                    "Budgettet må højst have to decimaler."
            );
        }
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    "Rejsen mangler gyldige datoer."
            );
        }
    }

    private Trip requireTrip(
            Long tripId,
            Long userId
    ) {
        return tripDAO.findByIdAndUserId(tripId, userId)
                .orElseThrow(() ->
                        new NotFoundResponse(
                                "Rejsen findes ikke."
                        )
                );
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

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return mapper.nullNode();
        }

        try {
            JsonNode result = mapper.readTree(value);

            if (result == null) {
                return mapper.nullNode();
            }

            return result;

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke læse rejseoplysninger.",
                    e
            );
        }
    }
}