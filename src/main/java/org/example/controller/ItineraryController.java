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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ItineraryController {

    private static final BigDecimal TEST_EUR_TO_DKK =
            new BigDecimal("7.50");

    private final TripDAO tripDAO = new TripDAO();
    private final ObjectMapper mapper = new ObjectMapper();

    public record BudgetInput(BigDecimal budgetAmount) {}

    public void registerRoutes(Javalin app) {

        // Hent samlet rejseplan og budget.
        app.get("/api/trips/{id}/itinerary", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            ctx.json(buildItinerary(requireTrip(tripId, userId)));
        });

        // Angiv eller opdater det maksimale rejsebudget.
        app.put("/api/trips/{id}/budget", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            requireTrip(tripId, userId);

            BudgetInput input = ctx.bodyAsClass(BudgetInput.class);
            BigDecimal amount = validateBudget(input.budgetAmount());

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> trip.updateBudget(amount)
            ).orElseThrow(this::tripNotFound);

            ObjectNode response = mapper.createObjectNode();
            response.put("tripId", tripId);
            response.put("message", "Budgettet er gemt.");
            response.set("budget", calculateBudget(updated));

            ctx.json(response);
        });

        // Fjern budgetgrænsen, men behold rejsens indhold.
        app.delete("/api/trips/{id}/budget", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> trip.updateBudget(null)
            ).orElseThrow(this::tripNotFound);

            ObjectNode response = mapper.createObjectNode();
            response.put("tripId", tripId);
            response.put("message", "Budgetgrænsen er fjernet.");
            response.set("budget", calculateBudget(updated));

            ctx.json(response);
        });
    }

    private ObjectNode buildItinerary(Trip trip) {
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
                    "Rejsen skal have gyldige start- og slutdatoer."
            );
        }

        List<JsonNode> activities = sortedActivities(trip);
        ArrayNode days = mapper.createArrayNode();

        for (LocalDate date = start;
             !date.isAfter(end);
             date = date.plusDays(1)) {

            String dateText = date.toString();
            ObjectNode day = mapper.createObjectNode();

            day.put("date", dateText);

            ArrayNode dailyActivities = mapper.createArrayNode();

            for (JsonNode activity : activities) {
                if (dateText.equals(activity.path("date").asText())) {
                    dailyActivities.add(activity);
                }
            }

            day.set("activities", dailyActivities);
            day.set("flights", flightsDepartingOn(flight, dateText));

            // Hotelovernatning hører til indtjekningsdagen
            // og de efterfølgende dage før udtjekning.
            boolean hotelNight = false;

            if (!hotel.isNull()) {
                LocalDate checkIn = parseDate(
                        hotel.path("checkIn").asText()
                );
                LocalDate checkOut = parseDate(
                        hotel.path("checkOut").asText()
                );

                hotelNight = !date.isBefore(checkIn)
                        && date.isBefore(checkOut);
            }

            day.put("hotelNight", hotelNight);

            if (hotelNight) {
                day.put("hotelName", hotel.path("name").asText());
                day.put("hotelId", hotel.path("id").asText());
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
        response.set("activities", mapper.valueToTree(activities));
        response.set("days", days);
        response.set("budget", calculateBudget(trip));

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

                if (departure.length() >= 10
                        && departure.substring(0, 10).equals(date)) {

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
        }

        return result;
    }

    private ObjectNode calculateBudget(Trip trip) {
        JsonNode flight = readJson(trip.getFlightJson());
        JsonNode hotel = readJson(trip.getHotelJson());

        BigDecimal flightAmount = BigDecimal.ZERO;
        BigDecimal hotelAmount = BigDecimal.ZERO;
        BigDecimal activityAmount = BigDecimal.ZERO;

        boolean flightKnown = true;
        boolean hotelKnown = true;
        boolean estimatedConversion = false;

        ArrayNode unknownPrices = mapper.createArrayNode();
        int unknownActivityCount = 0;

        if (!flight.isNull()) {
            BigDecimal converted = amountInDkk(
                    flight.path("total_amount"),
                    flight.path("total_currency").asText()
            );

            if (converted == null) {
                flightKnown = false;
                unknownPrices.add("Flypris mangler eller valuta understøttes ikke.");
            } else {
                flightAmount = converted;
                estimatedConversion = "EUR".equals(
                        flight.path("total_currency").asText()
                );
            }
        }

        if (!hotel.isNull()) {
            BigDecimal converted = amountInDkk(
                    hotel.path("totalAmount"),
                    hotel.path("currency").asText()
            );

            if (converted == null) {
                hotelKnown = false;
                unknownPrices.add(
                        "Hotelpris mangler eller valuta understøttes ikke."
                );
            } else {
                hotelAmount = converted;
                estimatedConversion |= "EUR".equals(
                        hotel.path("currency").asText()
                );
            }
        }

        for (JsonNode activity : sortedActivities(trip)) {
            BigDecimal converted = amountInDkk(
                    activity.path("priceAmount"),
                    activity.path("currency").asText()
            );

            if (converted == null) {
                unknownActivityCount++;
                unknownPrices.add(
                        "Aktivitet: " + activity.path("name").asText()
                );
            } else {
                activityAmount = activityAmount.add(converted);
                estimatedConversion |= "EUR".equals(
                        activity.path("currency").asText()
                );
            }
        }

        BigDecimal knownTotal = money(
                flightAmount.add(hotelAmount).add(activityAmount)
        );

        BigDecimal limit = trip.getBudgetAmount();
        boolean complete = flightKnown
                && hotelKnown
                && unknownActivityCount == 0;

        ObjectNode budget = mapper.createObjectNode();

        budget.put("currency", "DKK");
        budget.set("budgetAmount", mapper.valueToTree(limit));

        if (flightKnown) {
            budget.put("flightAmount", money(flightAmount));
        } else {
            budget.putNull("flightAmount");
        }

        if (hotelKnown) {
            budget.put("hotelAmount", money(hotelAmount));
        } else {
            budget.putNull("hotelAmount");
        }

        budget.put("knownActivitiesAmount", money(activityAmount));
        budget.put("knownTotalAmount", knownTotal);
        budget.put("priceComplete", complete);
        budget.put("unknownActivityPriceCount", unknownActivityCount);
        budget.set("unknownPrices", unknownPrices);
        budget.put("estimatedConversion", estimatedConversion);

        // Fravalgte fly/hoteller tæller som 0.
        // Ukendte priser tæller ikke som gratis.
        if (complete) {
            budget.put("totalAmount", knownTotal);
        } else {
            budget.putNull("totalAmount");
        }

        if (limit != null && complete) {
            BigDecimal remaining = money(limit.subtract(knownTotal));

            budget.put("remainingBudget", remaining);
            budget.put("overBudget", remaining.signum() < 0);
        } else {
            budget.putNull("remainingBudget");
            budget.putNull("overBudget");
        }

        // Brugeren kan stadig se, hvor meget de kendte
        // udgifter efterlader, når enkelte priser mangler.
        if (limit != null) {
            budget.put(
                    "remainingAfterKnownCosts",
                    money(limit.subtract(knownTotal))
            );
            budget.put(
                    "knownCostsExceedBudget",
                    knownTotal.compareTo(limit) > 0
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

    private BigDecimal amountInDkk(
            JsonNode amountNode,
            String currency
    ) {
        if (amountNode.isMissingNode() || amountNode.isNull()) {
            return null;
        }

        try {
            BigDecimal amount = new BigDecimal(amountNode.asText());

            if (amount.signum() < 0) {
                return null;
            }

            if ("DKK".equals(currency)) {
                return money(amount);
            }

            if ("EUR".equals(currency)) {
                return money(amount.multiply(TEST_EUR_TO_DKK));
            }

            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<JsonNode> sortedActivities(Trip trip) {
        JsonNode activities = readJson(trip.getActivitiesJson());
        List<JsonNode> sorted = new ArrayList<>();

        if (activities.isNull()) {
            return sorted;
        }

        if (!activities.isArray()) {
            throw new IllegalStateException(
                    "De gemte aktiviteter har et ugyldigt format."
            );
        }

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

        return sorted;
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
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new BadRequestResponse(
                    "Budgettet må højst have to decimaler."
            );
        }
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
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
}