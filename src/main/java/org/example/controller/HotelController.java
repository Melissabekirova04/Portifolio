package org.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.BadGatewayResponse;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.example.security.SecurityMiddleware;
import org.example.service.HotelService;
import org.example.service.HotelService.HotelOffer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class HotelController {

    // Fast testkurs. Ikke en aktuel valutakurs.
    private static final BigDecimal TEST_EUR_TO_DKK =
            new BigDecimal("7.50");

    private final HotelService hotelService = new HotelService();
    private final ObjectMapper mapper = new ObjectMapper();

    public record HotelChoice(String hotelId) {}

    private record Stay(
            String destination,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults
    ) {}

    public void registerRoutes(Javalin app) {

        app.post("/api/hotels/search", ctx -> {
            SecurityMiddleware.requireUserId(ctx);
            Stay stay = requireStay(ctx);

            List<HotelOffer> offers = hotelService.search(
                    stay.destination(),
                    stay.checkIn(),
                    stay.checkOut(),
                    stay.adults()
            );

            ctx.sessionAttribute("hotelOffers", offers);
            currentHotel(ctx, stay);

            ctx.json(Map.of(
                    "testData", true,
                    "currency", "DKK",
                    "message", "Fiktive hoteller og priser i danske kroner.",
                    "offers", offers
            ));
        });

        app.post("/api/trip-draft/hotel", ctx -> {
            SecurityMiddleware.requireUserId(ctx);
            Stay stay = requireStay(ctx);

            HotelChoice input = ctx.bodyAsClass(HotelChoice.class);

            if (input.hotelId() == null || input.hotelId().isBlank()) {
                throw new BadRequestResponse("hotelId mangler.");
            }

            List<HotelOffer> offers =
                    ctx.sessionAttribute("hotelOffers");

            if (offers == null || offers.isEmpty()) {
                throw new BadRequestResponse(
                        "Søg efter hoteller først."
                );
            }

            if (offers.stream().anyMatch(
                    offer -> !matches(offer, stay)
                            || !"DKK".equals(offer.currency())
            )) {
                clearHotel(ctx);

                throw new BadRequestResponse(
                        "Hoteltilbuddene skal opdateres. Søg hoteller igen."
                );
            }

            HotelOffer selected = offers.stream()
                    .filter(offer -> offer.id().equals(input.hotelId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundResponse(
                            "Hotellet findes ikke i din seneste søgning."
                    ));

            HotelOffer previous = currentHotel(ctx, stay);

            BigDecimal previousAmount = previous == null
                    ? BigDecimal.ZERO
                    : previous.totalAmount();

            BigDecimal change = selected.totalAmount()
                    .subtract(previousAmount);

            ctx.sessionAttribute("selectedHotel", selected);

            ObjectNode response = summary(ctx, stay);

            response.put(
                    "message",
                    previous == null
                            ? "Hotellet er tilføjet."
                            : "Hotelvalget er opdateret."
            );

            response.put("hotelPriceChange", change);
            response.put("hotelPriceChangeCurrency", "DKK");

            ctx.json(response);
        });

        app.delete("/api/trip-draft/hotel", ctx -> {
            SecurityMiddleware.requireUserId(ctx);
            Stay stay = requireStay(ctx);

            HotelOffer previous = currentHotel(ctx, stay);

            ctx.sessionAttribute("selectedHotel", null);

            ObjectNode response = summary(ctx, stay);

            response.put("message", "Hotellet er fjernet.");

            response.put(
                    "hotelPriceChange",
                    previous == null
                            ? BigDecimal.ZERO
                            : previous.totalAmount().negate()
            );

            response.put("hotelPriceChangeCurrency", "DKK");

            ctx.json(response);
        });

        app.get("/api/trip-draft/summary", ctx -> {
            SecurityMiddleware.requireUserId(ctx);

            ctx.json(summary(ctx, requireStay(ctx)));
        });

        // Ryd hotelvalg efter en vellykket ændring eller gemning.
        app.after(ctx -> {
            int statusCode = ctx.res().getStatus();

            if (statusCode < 200 || statusCode >= 300) {
                return;
            }

            String path = ctx.path();
            String method = ctx.req().getMethod();

            boolean newDestination =
                    method.equals("POST")
                            && path.equals("/api/trip-draft");

            boolean changedDetails =
                    method.equals("PUT")
                            && path.equals("/api/trip-draft/details");

            boolean savedTrip =
                    method.equals("POST")
                            && path.equals("/api/trips");

            if (newDestination || changedDetails || savedTrip) {
                clearHotel(ctx);
            }
        });
    }

    private void clearHotel(Context ctx) {
        ctx.sessionAttribute("hotelOffers", null);
        ctx.sessionAttribute("selectedHotel", null);
    }

    private Stay requireStay(Context ctx) {
        Object destination = ctx.sessionAttribute("tripDestination");
        Object search = ctx.sessionAttribute("flightSearch");

        if (destination == null || search == null) {
            throw new BadRequestResponse(
                    "Vælg destination og datoer først."
            );
        }

        JsonNode destinationData = mapper.valueToTree(destination);
        JsonNode searchData = mapper.valueToTree(search);

        String code = destinationData.path("airportCode").asText();

        if (!code.equals(searchData.path("destination").asText())) {
            throw new BadRequestResponse(
                    "Destinationen matcher ikke rejseoplysningerne."
            );
        }

        LocalDate checkIn;
        LocalDate checkOut;

        try {
            checkIn = LocalDate.parse(
                    searchData.path("departureDate").asText()
            );

            checkOut = LocalDate.parse(
                    searchData.path("returnDate").asText()
            );

        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    "Vælg gyldige start- og slutdatoer."
            );
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestResponse(
                    "Slutdatoen skal ligge efter startdatoen."
            );
        }

        if (checkIn.isBefore(
                LocalDate.now(ZoneId.of("Europe/Copenhagen"))
        )) {
            throw new BadRequestResponse(
                    "Startdatoen må ikke være i fortiden."
            );
        }

        int adults = searchData.path("adults").asInt(0);

        if (adults < 1 || adults > 9) {
            throw new BadRequestResponse(
                    "Vælg mellem 1 og 9 voksne."
            );
        }

        return new Stay(code, checkIn, checkOut, adults);
    }

    private HotelOffer currentHotel(Context ctx, Stay stay) {
        HotelOffer hotel = ctx.sessionAttribute("selectedHotel");

        if (hotel != null
                && (!matches(hotel, stay)
                || !"DKK".equals(hotel.currency()))) {

            ctx.sessionAttribute("selectedHotel", null);
            return null;
        }

        return hotel;
    }

    private boolean matches(HotelOffer hotel, Stay stay) {
        return hotel.destination().equals(stay.destination())
                && hotel.checkIn().equals(stay.checkIn().toString())
                && hotel.checkOut().equals(stay.checkOut().toString())
                && hotel.adults() == stay.adults();
    }

    private ObjectNode summary(Context ctx, Stay stay) {
        HotelOffer hotel = currentHotel(ctx, stay);
        JsonNode flight = ctx.sessionAttribute("selectedFlight");

        ObjectNode response = mapper.createObjectNode();

        response.put("destination", stay.destination());
        response.put("startDate", stay.checkIn().toString());
        response.put("endDate", stay.checkOut().toString());
        response.put("adults", stay.adults());
        response.put("currency", "DKK");

        BigDecimal flightDkk = BigDecimal.ZERO;
        boolean conversionSupported = true;
        boolean estimatedConversion = false;

        if (flight == null || flight.isNull()) {
            response.putNull("flight");
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

            String originalCurrency =
                    flight.path("total_currency").asText();

            if (originalCurrency.isBlank()) {
                throw new BadGatewayResponse(
                        "Flytilbuddet mangler valuta."
                );
            }

            ObjectNode flightSummary = mapper.createObjectNode();

            flightSummary.put("offerId", flight.path("id").asText());
            flightSummary.put("originalAmount", originalAmount);
            flightSummary.put("originalCurrency", originalCurrency);
            flightSummary.put(
                    "expiresAt",
                    flight.path("expires_at").asText()
            );

            if ("DKK".equals(originalCurrency)) {
                flightDkk = originalAmount.setScale(
                        2, RoundingMode.HALF_UP
                );

                flightSummary.put("totalAmount", flightDkk);
                flightSummary.put("currency", "DKK");
                flightSummary.put("estimatedConversion", false);

            } else if ("EUR".equals(originalCurrency)) {
                flightDkk = originalAmount
                        .multiply(TEST_EUR_TO_DKK)
                        .setScale(2, RoundingMode.HALF_UP);

                estimatedConversion = true;

                flightSummary.put("totalAmount", flightDkk);
                flightSummary.put("currency", "DKK");
                flightSummary.put("estimatedConversion", true);
                flightSummary.put("exchangeRate", TEST_EUR_TO_DKK);

            } else {
                conversionSupported = false;

                flightSummary.putNull("totalAmount");
                flightSummary.put("currency", "DKK");

                flightSummary.put(
                        "message",
                        "Omregning fra " + originalCurrency
                                + " er endnu ikke understøttet."
                );
            }

            response.set("flight", flightSummary);
        }

        response.set("hotel", mapper.valueToTree(hotel));

        BigDecimal hotelDkk = hotel == null
                ? BigDecimal.ZERO
                : hotel.totalAmount();

        if (conversionSupported) {
            response.put(
                    "totalAmount",
                    flightDkk.add(hotelDkk)
                            .setScale(2, RoundingMode.HALF_UP)
            );
        } else {
            response.putNull("totalAmount");

            response.put(
                    "message",
                    "Samlet pris kan ikke beregnes, før flyets "
                            + "valuta kan omregnes til DKK."
            );
        }

        response.put("estimatedConversion", estimatedConversion);

        response.put(
                "priceNotice",
                "Fiktive hotelpriser. EUR omregnes med fast "
                        + "testkurs på 7,50 DKK. Ingen reservation er foretaget."
        );

        return response;
    }
}