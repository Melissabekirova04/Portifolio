package org.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.BadGatewayResponse;
import io.javalin.http.BadRequestResponse;
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
    private final ObjectMapper mapper = new ObjectMapper();

    public record Destination(
            String city,
            String country,
            String airportCode,
            String searchNames
    ) {}

    public record TripChoice(String destination) {}

    public record FlightSearch(
            String origin,
            String destination,
            String departureDate,
            Integer adults
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
                    "Aalborg", "Denmark", "AAL",
                    "Danmark"
            ),
            new Destination(
                    "Billund", "Denmark", "BLL",
                    "Danmark"
            ),

            new Destination(
                    "Oslo", "Norway", "OSL",
                    "Norge"
            ),
            new Destination(
                    "Bergen", "Norway", "BGO",
                    "Norge"
            ),
            new Destination(
                    "Trondheim", "Norway", "TRD",
                    "Norge"
            ),
            new Destination(
                    "Stavanger", "Norway", "SVG",
                    "Norge"
            ),

            new Destination(
                    "Stockholm", "Sweden", "ARN",
                    "Sverige"
            ),
            new Destination(
                    "Gothenburg", "Sweden", "GOT",
                    "Göteborg Sverige"
            ),
            new Destination(
                    "Malmö", "Sweden", "MMX",
                    "Malmo Sverige"
            ),
            new Destination(
                    "Umeå", "Sweden", "UME",
                    "Umea Sverige"
            )
    );

    public void registerRoutes(Javalin app) {

        // Søg på by, land eller lufthavnskode.
        // Uden search vises alle 12 destinationer.
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

        // Vælg destination og start en ny rejsekladde.
        app.post("/api/trip-draft", ctx -> {
            TripChoice input = ctx.bodyAsClass(TripChoice.class);

            Destination destination =
                    findDestination(input.destination());

            ctx.sessionAttribute("tripDestination", destination);
            ctx.sessionAttribute("flightSearch", null);
            ctx.sessionAttribute("flightOffers", null);
            ctx.sessionAttribute("selectedFlight", null);

            ctx.status(201).json(destination);
        });

        // Søg efter enkeltbilletter på economy.
        app.post("/api/flights/search", ctx -> {
            FlightSearch input = ctx.bodyAsClass(FlightSearch.class);

            Destination selected =
                    ctx.sessionAttribute("tripDestination");

            if (selected == null) {
                throw new BadRequestResponse(
                        "Vælg først en destination via /api/trip-draft."
                );
            }

            Destination origin = findDestination(input.origin());
            Destination destination =
                    findDestination(input.destination());

            if (!destination.airportCode()
                    .equals(selected.airportCode())) {
                throw new BadRequestResponse(
                        "Destinationen skal matche din rejsekladde."
                );
            }

            if (origin.airportCode()
                    .equals(destination.airportCode())) {
                throw new BadRequestResponse(
                        "Afrejse og destination skal være forskellige."
                );
            }

            LocalDate date = parseDate(input.departureDate());

            if (date.isBefore(
                    LocalDate.now(ZoneId.of("Europe/Copenhagen"))
            )) {
                throw new BadRequestResponse(
                        "Rejsedatoen må ikke være i fortiden."
                );
            }

            int adults = input.adults() == null ? 1 : input.adults();

            if (adults < 1 || adults > 9) {
                throw new BadRequestResponse(
                        "Vælg mellem 1 og 9 voksne."
                );
            }

            // En ny søgning erstatter gamle tilbud og flyvalg.
            ctx.sessionAttribute("flightSearch", null);
            ctx.sessionAttribute("flightOffers", null);
            ctx.sessionAttribute("selectedFlight", null);

            JsonNode result = flightService.searchFlights(
                    origin.airportCode(),
                    destination.airportCode(),
                    date.toString(),
                    adults
            );

            FlightSearch validatedSearch = new FlightSearch(
                    origin.airportCode(),
                    destination.airportCode(),
                    date.toString(),
                    adults
            );

            ctx.sessionAttribute("flightSearch", validatedSearch);
            ctx.sessionAttribute("flightOffers", result.path("offers"));

            ctx.json(Map.of(
                    "liveMode", result.path("live_mode").asBoolean(),
                    "offers", result.path("offers")
            ));
        });

        // Vælg et flytilbud fra den seneste søgning.
        app.post("/api/trip-draft/flight", ctx -> {
            FlightChoice input = ctx.bodyAsClass(FlightChoice.class);

            if (input.offerId() == null || input.offerId().isBlank()) {
                throw new BadRequestResponse("offerId mangler.");
            }

            JsonNode offers = ctx.sessionAttribute("flightOffers");

            if (offers == null) {
                throw new BadRequestResponse("Søg efter fly først.");
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
                        "Tilbuddet findes ikke i din seneste søgning."
                );
            }

            String expiresAt =
                    selectedOffer.path("expires_at").asText();

            Instant expiration;

            try {
                expiration = Instant.parse(expiresAt);
            } catch (DateTimeParseException e) {
                throw new BadGatewayResponse(
                        "Tilbuddet har en ugyldig udløbstid."
                );
            }

            if (!expiration.isAfter(Instant.now())) {
                throw new BadRequestResponse(
                        "Tilbuddet er udløbet. Søg efter fly igen."
                );
            }

            ctx.sessionAttribute("selectedFlight", selectedOffer);
            ctx.json(selectedOffer);
        });

        // Se destination, søgning og valgt fly.
        app.get("/api/trip-draft", ctx -> {
            Destination destination =
                    ctx.sessionAttribute("tripDestination");

            if (destination == null) {
                throw new NotFoundResponse(
                        "Du har ingen rejsekladde."
                );
            }

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

    private Destination findDestination(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestResponse("Lufthavnskode mangler.");
        }

        return destinations.stream()
                .filter(destination ->
                        destination.airportCode()
                                .equalsIgnoreCase(code.trim())
                )
                .findFirst()
                .orElseThrow(() -> new BadRequestResponse(
                        "Ukendt lufthavn. Brug /api/destinations."
                ));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestResponse("departureDate mangler.");
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    "departureDate skal have formatet YYYY-MM-DD."
            );
        }
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