
package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.javalin.Javalin;
import io.javalin.http.BadGatewayResponse;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.example.service.FlightService;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DestinationController {

    private final FlightService flightService = new FlightService();

    private final ObjectMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public record Destination(
            String city,
            String country,
            String airportCode,
            String searchNames
    ) {}

    public record TripChoice(String destination) {}

    public record TripDetails(
            String origin,
            String startDate,
            String endDate,
            Integer adults,
            Boolean directOnly
    ) {}

    public record FlightSearch(
            String origin,
            String destination,
            String departureDate,
            String returnDate,
            Integer adults,
            boolean directOnly
    ) {}

    public record FlightChoice(String offerId) {}

    private final List<Destination> destinations = List.of(
            new Destination(
                    "Copenhagen", "Denmark", "CPH",
                    "København Danmark"
            ),
            new Destination(
                    "Aarhus", "Denmark", "AAR",
                    "Århus Danmark"
            ),
            new Destination(
                    "Aalborg", "Denmark", "AAL", "Danmark"
            ),
            new Destination(
                    "Billund", "Denmark", "BLL", "Danmark"
            ),
            new Destination(
                    "Oslo", "Norway", "OSL", "Norge"
            ),
            new Destination(
                    "Bergen", "Norway", "BGO", "Norge"
            ),
            new Destination(
                    "Trondheim", "Norway", "TRD", "Norge"
            ),
            new Destination(
                    "Stavanger", "Norway", "SVG", "Norge"
            ),
            new Destination(
                    "Stockholm", "Sweden", "ARN", "Sverige"
            ),
            new Destination(
                    "Gothenburg", "Sweden", "GOT",
                    "Göteborg Sverige"
            ),
            new Destination(
                    "Malmö", "Sweden", "MMX", "Malmo Sverige"
            ),
            new Destination(
                    "Umeå", "Sweden", "UME", "Umea Sverige"
            )
    );

    public void registerRoutes(Javalin app) {
        app.get("/api/destinations", ctx -> {
            String search = ctx.queryParam("search");
            String term = normalize(search == null ? "" : search);

            List<Destination> results = destinations.stream()
                    .filter(destination -> normalize(
                            destination.city() + " "
                                    + destination.country() + " "
                                    + destination.airportCode() + " "
                                    + destination.searchNames()
                    ).contains(term))
                    .toList();

            ctx.json(results);
        });

        app.post("/api/trip-draft", ctx -> {
            TripChoice input = readRequest(ctx, TripChoice.class);

            Destination destination =
                    findDestination(input.destination());

            ctx.sessionAttribute("tripDestination", destination);
            ctx.sessionAttribute("flightSearch", null);

            clearFlightSelection(ctx);

            ctx.status(201).json(destination);
        });

        app.put("/api/trip-draft/details", ctx -> {
            Destination destination = requireDestination(ctx);
            TripDetails input = readRequest(ctx, TripDetails.class);

            Destination origin = findDestination(input.origin());

            if (origin.airportCode()
                    .equals(destination.airportCode())) {
                throw new BadRequestResponse(
                        "Du skal vælge forskellige lufthavne til afrejse og destination."
                );
            }

            LocalDate startDate = parseDate(
                    input.startDate(), "startDate"
            );

            LocalDate endDate = parseDate(
                    input.endDate(), "endDate"
            );

            if (startDate.isBefore(today())) {
                throw new BadRequestResponse(
                        "Afrejsedatoen må ikke ligge i fortiden."
                );
            }

            if (!endDate.isAfter(startDate)) {
                throw new BadRequestResponse(
                        "Hjemrejsedatoen skal ligge efter afrejsedatoen."
                );
            }

            int adults = input.adults() == null ? 1 : input.adults();

            if (adults < 1 || adults > 9) {
                throw new BadRequestResponse(
                        "Du skal vælge mellem 1 og 9 voksne."
                );
            }

            boolean directOnly =
                    Boolean.TRUE.equals(input.directOnly());

            FlightSearch search = new FlightSearch(
                    origin.airportCode(),
                    destination.airportCode(),
                    startDate.toString(),
                    endDate.toString(),
                    adults,
                    directOnly
            );

            ctx.sessionAttribute("flightSearch", search);
            clearFlightSelection(ctx);

            ctx.json(search);
        });

        app.post("/api/flights/search", ctx -> {
            Destination destination = requireDestination(ctx);

            FlightSearch search =
                    ctx.sessionAttribute("flightSearch");

            if (search == null) {
                throw new BadRequestResponse(
                        "Vælg først afrejselufthavn og rejsedatoer "
                                + "for din rejse."
                );
            }

            if (!destination.airportCode()
                    .equals(search.destination())) {
                throw new BadRequestResponse(
                        "Destinationen er blevet ændret. Vælg dine rejsedatoer igen."
                );
            }

            LocalDate departureDate = parseDate(
                    search.departureDate(), "startDate"
            );

            if (departureDate.isBefore(today())) {
                throw new BadRequestResponse(
                        "Afrejsedatoen er passeret. Vælg nye rejsedatoer."
                );
            }

            clearFlightSelection(ctx);

            JsonNode result = flightService.searchFlights(
                    search.origin(),
                    search.destination(),
                    search.departureDate(),
                    search.returnDate(),
                    search.adults(),
                    search.directOnly()
            );

            JsonNode offers = result.path("offers");
            ctx.sessionAttribute("flightOffers", offers);

            String message;

            if (offers.isEmpty()) {
                message = search.directOnly()
                        ? "Vi fandt ingen direkte fly til din søgning. "
                        + "Prøv andre datoer eller tillad mellemlandinger."
                        : "Vi fandt ingen fly til din søgning. Prøv andre datoer.";
            } else {
                message = "Vi har fundet flytilbud til din rejse.";
            }

            ctx.json(Map.of(
                    "liveMode", result.path("live_mode").asBoolean(),
                    "search", search,
                    "offers", offers,
                    "message", message
            ));
        });

        app.post("/api/trip-draft/flight", ctx -> {
            requireDestination(ctx);

            FlightChoice input =
                    readRequest(ctx, FlightChoice.class);

            if (input.offerId() == null || input.offerId().isBlank()) {
                throw new BadRequestResponse(
                        "Du skal vælge et flytilbud."
                );
            }

            JsonNode offers = ctx.sessionAttribute("flightOffers");

            if (offers == null) {
                throw new BadRequestResponse(
                        "Du skal søge efter fly, før du kan vælge et tilbud."
                );
            }

            JsonNode selectedOffer = null;

            for (JsonNode offer : offers) {
                if (input.offerId().equals(offer.path("id").asText())) {
                    selectedOffer = offer;
                    break;
                }
            }

            if (selectedOffer == null) {
                throw new NotFoundResponse(
                        "Flytilbuddet findes ikke længere i din seneste søgning. Søg efter fly igen."
                );
            }

            Instant expiration;

            try {
                expiration = Instant.parse(
                        selectedOffer.path("expires_at").asText()
                );
            } catch (DateTimeParseException e) {
                throw new BadGatewayResponse(
                        "Vi kunne ikke kontrollere flytilbuddets gyldighed. Prøv at søge igen."
                );
            }

            if (!expiration.isAfter(Instant.now())) {
                throw new BadRequestResponse(
                        "Flytilbuddet er udløbet. Søg efter fly igen."
                );
            }

            ctx.sessionAttribute("selectedFlight", selectedOffer);
            ctx.json(selectedOffer);
        });

        app.get("/api/trip-draft", ctx -> {
            Destination destination = requireDestination(ctx);

            FlightSearch search =
                    ctx.sessionAttribute("flightSearch");

            JsonNode flight =
                    ctx.sessionAttribute("selectedFlight");

            var draft = mapper.createObjectNode();

            draft.set("destination", mapper.valueToTree(destination));
            draft.set("search", mapper.valueToTree(search));
            draft.set("flight", mapper.valueToTree(flight));

            ctx.json(draft);
        });
    }

    private <T> T readRequest(Context ctx, Class<T> type)
            throws JsonProcessingException {

        try {
            T input = mapper.readValue(ctx.body(), type);

            if (input == null) {
                throw new BadRequestResponse(
                        "Du skal udfylde de nødvendige oplysninger."
                );
            }

            return input;

        } catch (StreamReadException | MismatchedInputException e) {
            throw new BadRequestResponse(
                    "Nogle af oplysningerne er ugyldige. Kontrollér felterne, og prøv igen."
            );
        }
    }

    private void clearFlightSelection(Context ctx) {
        ctx.sessionAttribute("flightOffers", null);
        ctx.sessionAttribute("selectedFlight", null);
    }

    private Destination requireDestination(Context ctx) {
        Destination destination =
                ctx.sessionAttribute("tripDestination");

        if (destination == null) {
            throw new BadRequestResponse(
                    "Du skal vælge en destination, før du kan fortsætte."
            );
        }

        return destination;
    }

    private Destination findDestination(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestResponse(
                    "Du skal vælge en lufthavn fra listen."
            );
        }

        return destinations.stream()
                .filter(destination ->
                        destination.airportCode()
                                .equalsIgnoreCase(code.trim())
                )
                .findFirst()
                .orElseThrow(() -> new BadRequestResponse(
                        "Destinationen findes ikke. Vælg en destination fra listen."
                ));
    }

    private LocalDate parseDate(String value, String field) {
        String label = switch (field) {
            case "startDate" -> "Afrejsedatoen";
            case "endDate" -> "Hjemrejsedatoen";
            default -> "Datoen";
        };

        if (value == null || value.isBlank()) {
            throw new BadRequestResponse(
                    "Du skal angive " + label.toLowerCase(Locale.ROOT) + "."
            );
        }

        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BadRequestResponse(
                    label + " skal angives i formatet ÅÅÅÅ-MM-DD."
            );
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    label + " er ikke en gyldig dato."
            );
        }
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Europe/Copenhagen"));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(
                value.toLowerCase(Locale.ROOT)
                        .trim()
                        .replace("ø", "o")
                        .replace("æ", "ae"),
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");
    }
}
