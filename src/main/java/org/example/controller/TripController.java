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

public class TripController {

    // Samme faste testkurs som i HotelController.
    private static final BigDecimal TEST_EUR_TO_DKK =
            new BigDecimal("7.50");

    private final TripDAO tripDAO = new TripDAO();
    private final HotelService hotelService = new HotelService();
    private final ObjectMapper mapper = new ObjectMapper();

    public void registerRoutes(Javalin app) {

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

            validateDates(search);

            JsonNode destinationData = mapper.valueToTree(destination);
            JsonNode searchData = mapper.valueToTree(search);

            String code = destinationData.path("airportCode").asText();

            if (!code.equals(
                    searchData.path("destination").asText()
            )) {
                throw new BadRequestResponse(
                        "Destinationen matcher ikke rejseoplysningerne."
                );
            }

            int adults = searchData.path("adults").asInt();

            if (adults < 1 || adults > 9) {
                throw new BadRequestResponse(
                        "Vælg mellem 1 og 9 voksne."
                );
            }

            validateFlight(flight);

            if (hotel != null) {
                validateHotel(code, searchData, hotel);
            }

            ObjectNode priceSummary = createPriceSummary(
                    flight, hotel
            );

            Trip trip = tripDAO.create(
                    userId,
                    mapper.writeValueAsString(destination),
                    mapper.writeValueAsString(search),
                    toJsonOrNull(flight),
                    toJsonOrNull(hotel),
                    mapper.writeValueAsString(priceSummary)
            );

            // Ryd først kladden, når databasen har gemt rejsen.
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

        JsonNode data = mapper.valueToTree(search);

        LocalDate startDate;
        LocalDate endDate;

        try {
            startDate = LocalDate.parse(
                    data.path("departureDate").asText()
            );

            endDate = LocalDate.parse(
                    data.path("returnDate").asText()
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
                            + "eller fjern flyet før gemning."
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

        // Kontrollér, at hotel og pris stadig passer til kladden.
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
            HotelOffer hotel
    ) {
        ObjectNode summary = mapper.createObjectNode();

        summary.put("currency", "DKK");
        summary.put("calculatedAt", Instant.now().toString());
        summary.put("hotelTestData", hotel != null);

        BigDecimal hotelAmount = hotel == null
                ? BigDecimal.ZERO
                : hotel.totalAmount();

        BigDecimal flightDkk = BigDecimal.ZERO;
        boolean supported = true;
        boolean estimatedConversion = false;

        summary.put("hotelAmount", money(hotelAmount));

        if (flight == null || flight.isNull()) {
            summary.putNull("flightOriginalAmount");
            summary.putNull("flightOriginalCurrency");
            summary.putNull("eurToDkkRate");

        } else {
            BigDecimal originalAmount;

            try {
                originalAmount = new BigDecimal(
                        flight.path("total_amount").asText()
                );
            } catch (NumberFormatException e) {
                throw new BadGatewayResponse(
                        "Flytilbuddet har en ugyldig pris."
                );
            }

            if (originalAmount.signum() < 0) {
                throw new BadGatewayResponse(
                        "Flytilbuddet har en ugyldig pris."
                );
            }

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
                summary.putNull("eurToDkkRate");

            } else if ("EUR".equals(currency)) {
                flightDkk = originalAmount.multiply(TEST_EUR_TO_DKK);
                estimatedConversion = true;

                summary.put("eurToDkkRate", TEST_EUR_TO_DKK);

            } else {
                supported = false;
                summary.putNull("eurToDkkRate");
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