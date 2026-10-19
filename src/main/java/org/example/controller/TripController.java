package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.BadGatewayResponse;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import org.example.dao.TripDAO;
import org.example.entity.Trip;
import org.example.security.SecurityMiddleware;
import org.example.service.FlightService;
import org.example.service.HotelService;
import org.example.service.HotelService.HotelOffer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TripController {

    private static final BigDecimal TEST_EUR_TO_DKK =
            new BigDecimal("7.50");

    private final TripDAO tripDAO = new TripDAO();
    private final HotelService hotelService = new HotelService();
    private final FlightService flightService = new FlightService();
    private final ObjectMapper mapper = new ObjectMapper();

    public record FlightChoice(String offerId) {}

    // Søgeresultater til én bestemt gemt rejse.
    public record SavedFlightSearch(
            Long tripId,
            Long userId,
            String searchJson,
            JsonNode offers
    ) {}

    public void registerRoutes(Javalin app) {

        // Opret en rejse fra den aktuelle kladde.
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
            JsonNode flight = ctx.sessionAttribute("selectedFlight");
            HotelOffer hotel = ctx.sessionAttribute("selectedHotel");

            JsonNode searchData = mapper.valueToTree(search);

            validateSearch(searchData);
            validateFlight(flight);

            JsonNode destinationData =
                    mapper.valueToTree(destination);

            String code =
                    destinationData.path("airportCode").asText();

            if (!code.equals(
                    searchData.path("destination").asText()
            )) {
                throw new BadRequestResponse(
                        "Destinationen matcher ikke rejseoplysningerne."
                );
            }

            if (hotel != null) {
                validateHotel(code, searchData, hotel);
            }

            JsonNode hotelData = mapper.valueToTree(hotel);

            ObjectNode priceSummary =
                    createPriceSummary(flight, hotelData);

            Trip trip = tripDAO.create(
                    userId,
                    toJson(destination),
                    toJson(search),
                    toJsonOrNull(flight),
                    toJsonOrNull(hotel),
                    toJson(priceSummary)
            );

            ctx.sessionAttribute("tripDestination", null);
            ctx.sessionAttribute("flightSearch", null);
            ctx.sessionAttribute("flightOffers", null);
            ctx.sessionAttribute("selectedFlight", null);
            ctx.sessionAttribute("hotelOffers", null);
            ctx.sessionAttribute("selectedHotel", null);

            ctx.status(201).json(toResponse(trip));
        });

        app.get("/api/trips", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);

            List<ObjectNode> response = new ArrayList<>();

            for (Trip trip : tripDAO.findByUserId(userId)) {
                response.add(toResponse(trip));
            }

            ctx.json(response);
        });

        app.get("/api/trips/{id}", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            ctx.json(toResponse(requireTrip(tripId, userId)));
        });

        // US7: Søg nye fly til en allerede gemt rejse.
        app.post("/api/trips/{id}/flights/search", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip trip = requireTrip(tripId, userId);

            JsonNode search = readJsonOrNull(trip.getSearchJson());
            validateSearch(search);

            // Fjern gamle søgeresultater, men behold det gemte fly.
            ctx.sessionAttribute("savedTripFlightSearch", null);

            JsonNode result = flightService.searchFlights(
                    search.path("origin").asText(),
                    search.path("destination").asText(),
                    search.path("departureDate").asText(),
                    search.path("returnDate").asText(),
                    search.path("adults").asInt(),
                    search.path("directOnly").asBoolean()
            );

            JsonNode offers = result.path("offers");

            ctx.sessionAttribute(
                    "savedTripFlightSearch",
                    new SavedFlightSearch(
                            tripId,
                            userId,
                            trip.getSearchJson(),
                            offers
                    )
            );

            ctx.json(Map.of(
                    "tripId", tripId,
                    "search", search,
                    "liveMode", result.path("live_mode").asBoolean(),
                    "offers", offers,
                    "message", offers.isEmpty()
                            ? "Ingen flytilbud matcher rejsen."
                            : "Flytilbud fundet."
            ));
        });

        // US7: Tilføj eller erstat flyet på den gemte rejse.
        app.put("/api/trips/{id}/flight", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            // Kontrollér ejerskab før behandling af valget.
            requireTrip(tripId, userId);

            FlightChoice input =
                    ctx.bodyAsClass(FlightChoice.class);

            if (input.offerId() == null || input.offerId().isBlank()) {
                throw new BadRequestResponse("offerId mangler.");
            }

            SavedFlightSearch cached =
                    ctx.sessionAttribute("savedTripFlightSearch");

            if (cached == null
                    || !tripId.equals(cached.tripId())
                    || !userId.equals(cached.userId())) {
                throw new BadRequestResponse(
                        "Søg først fly via /api/trips/"
                                + tripId + "/flights/search."
                );
            }

            JsonNode selected = findOffer(
                    cached.offers(),
                    input.offerId()
            );

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> {
                        if (!Objects.equals(
                                trip.getSearchJson(),
                                cached.searchJson()
                        )) {
                            throw new BadRequestResponse(
                                    "Rejseoplysningerne er ændret. "
                                            + "Søg efter fly igen."
                            );
                        }

                        validateSearch(
                                readJsonOrNull(trip.getSearchJson())
                        );

                        validateFlight(selected);

                        JsonNode hotel =
                                readJsonOrNull(trip.getHotelJson());

                        ObjectNode summary =
                                createPriceSummary(selected, hotel);

                        trip.updateFlight(
                                toJson(selected),
                                toJson(summary)
                        );
                    }
            ).orElseThrow(this::tripNotFound);

            ctx.json(toResponse(updated));
        });

        // US7: Fjern flyet fra den gemte rejse.
        app.delete("/api/trips/{id}/flight", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip updated = tripDAO.updateOwnedTrip(
                    tripId,
                    userId,
                    trip -> {
                        JsonNode hotel =
                                readJsonOrNull(trip.getHotelJson());

                        ObjectNode summary =
                                createPriceSummary(null, hotel);

                        trip.updateFlight(null, toJson(summary));
                    }
            ).orElseThrow(this::tripNotFound);

            ctx.json(toResponse(updated));
        });
    }

    private Trip requireTrip(Long tripId, Long userId) {
        return tripDAO.findByIdAndUserId(tripId, userId)
                .orElseThrow(this::tripNotFound);
    }

    private NotFoundResponse tripNotFound() {
        return new NotFoundResponse("Rejsen findes ikke.");
    }

    private JsonNode findOffer(JsonNode offers, String offerId) {
        for (JsonNode offer : offers) {
            if (offerId.equals(offer.path("id").asText())) {
                return offer;
            }
        }

        throw new NotFoundResponse(
                "Tilbuddet findes ikke i din seneste søgning "
                        + "til denne rejse."
        );
    }

    private void validateSearch(JsonNode search) {
        if (search == null || search.isNull()) {
            throw new BadRequestResponse(
                    "Rejsen mangler datoer og flysøgningsoplysninger."
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

        String origin = search.path("origin").asText();
        String destination = search.path("destination").asText();

        if (origin.isBlank()
                || destination.isBlank()
                || origin.equals(destination)) {
            throw new BadRequestResponse(
                    "Rejsen skal have forskellige afrejse- "
                            + "og destinationslufthavne."
            );
        }

        int adults = search.path("adults").asInt();

        if (adults < 1 || adults > 9) {
            throw new BadRequestResponse(
                    "Vælg mellem 1 og 9 voksne."
            );
        }
    }

    private void validateFlight(JsonNode flight) {
        if (flight == null || flight.isNull()) {
            return;
        }

        Instant expiration;

        try {
            expiration = Instant.parse(
                    flight.path("expires_at").asText()
            );

        } catch (DateTimeParseException e) {
            throw new BadGatewayResponse(
                    "Flytilbuddet har en ugyldig udløbstid."
            );
        }

        if (!expiration.isAfter(Instant.now())) {
            throw new BadRequestResponse(
                    "Flytilbuddet er udløbet. Søg efter fly igen "
                            + "eller fjern flyet."
            );
        }

        if (!flight.path("slices").isArray()
                || flight.path("slices").size() != 2) {
            throw new BadGatewayResponse(
                    "Flytilbuddet skal indeholde ud- og hjemrejse."
            );
        }
    }

    private void validateHotel(
            String destination,
            JsonNode search,
            HotelOffer selected
    ) {
        LocalDate checkIn = LocalDate.parse(
                search.path("departureDate").asText()
        );

        LocalDate checkOut = LocalDate.parse(
                search.path("returnDate").asText()
        );

        int adults = search.path("adults").asInt();

        boolean valid = hotelService.search(
                        destination, checkIn, checkOut, adults
                )
                .stream()
                .anyMatch(offer -> offer.equals(selected));

        if (!valid) {
            throw new BadRequestResponse(
                    "Hotelvalget matcher ikke længere rejsen. "
                            + "Søg hoteller og vælg igen."
            );
        }
    }

    private ObjectNode createPriceSummary(
            JsonNode flight,
            JsonNode hotel
    ) {
        ObjectNode summary = mapper.createObjectNode();

        boolean hasHotel = hotel != null && !hotel.isNull();

        BigDecimal hotelAmount = BigDecimal.ZERO;

        if (hasHotel) {
            if (!"DKK".equals(hotel.path("currency").asText())) {
                throw new BadRequestResponse(
                        "Hotelprisen skal være i DKK."
                );
            }

            hotelAmount = readAmount(
                    hotel.path("totalAmount").asText(),
                    "Hotel"
            );
        }

        summary.put("currency", "DKK");
        summary.put("calculatedAt", Instant.now().toString());
        summary.put(
                "hotelTestData",
                hasHotel && hotel.path("testData").asBoolean()
        );
        summary.put("hotelAmount", money(hotelAmount));

        BigDecimal flightDkk = BigDecimal.ZERO;
        boolean supported = true;
        boolean estimatedConversion = false;

        summary.putNull("flightOriginalAmount");
        summary.putNull("flightOriginalCurrency");
        summary.putNull("eurToDkkRate");

        if (flight != null && !flight.isNull()) {
            BigDecimal originalAmount = readAmount(
                    flight.path("total_amount").asText(),
                    "Fly"
            );

            String currency =
                    flight.path("total_currency").asText();

            if (currency.isBlank()) {
                throw new BadGatewayResponse(
                        "Flytilbuddet mangler valuta."
                );
            }

            summary.put("flightOriginalAmount", originalAmount);
            summary.put("flightOriginalCurrency", currency);

            if ("DKK".equals(currency)) {
                flightDkk = originalAmount;

            } else if ("EUR".equals(currency)) {
                flightDkk = originalAmount.multiply(TEST_EUR_TO_DKK);
                estimatedConversion = true;
                summary.put("eurToDkkRate", TEST_EUR_TO_DKK);

            } else {
                supported = false;
            }
        }

        if (supported) {
            BigDecimal roundedFlight = money(flightDkk);

            summary.put("flightAmount", roundedFlight);
            summary.put(
                    "totalAmount",
                    money(roundedFlight.add(hotelAmount))
            );

        } else {
            summary.putNull("flightAmount");
            summary.putNull("totalAmount");

            summary.put(
                    "message",
                    "Flyets valuta understøttes endnu ikke til DKK-omregning."
            );
        }

        summary.put("estimatedConversion", estimatedConversion);

        summary.put(
                "priceNotice",
                "Gemt planlægningspris. Fiktive hotelpriser. "
                        + "EUR omregnes med fast testkurs på 7,50 DKK. "
                        + "Ingen reservation er foretaget."
        );

        return summary;
    }

    private BigDecimal readAmount(String value, String label) {
        try {
            BigDecimal amount = new BigDecimal(value);

            if (amount.signum() < 0) {
                throw new NumberFormatException();
            }

            return amount;

        } catch (NumberFormatException e) {
            throw new BadGatewayResponse(
                    label + " har en ugyldig pris."
            );
        }
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
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

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke serialisere rejseoplysninger.", e
            );
        }
    }

    private String toJsonOrNull(Object value) {
        if (value == null
                || (value instanceof JsonNode node && node.isNull())) {
            return null;
        }

        return toJson(value);
    }

    private JsonNode readJsonOrNull(String value) {
        if (value == null) {
            return mapper.nullNode();
        }

        try {
            return mapper.readTree(value);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke læse gemte rejseoplysninger.", e
            );
        }
    }

    private ObjectNode toResponse(Trip trip) {
        ObjectNode response = mapper.createObjectNode();

        response.put("id", trip.getId());
        response.put("createdAt", trip.getCreatedAt().toString());

        response.set(
                "destination",
                readJsonOrNull(trip.getDestinationJson())
        );

        response.set(
                "search",
                readJsonOrNull(trip.getSearchJson())
        );

        response.set(
                "flight",
                readJsonOrNull(trip.getFlightJson())
        );

        response.set(
                "hotel",
                readJsonOrNull(trip.getHotelJson())
        );

        response.set(
                "priceSummary",
                readJsonOrNull(trip.getPriceSummaryJson())
        );

        return response;
    }
}