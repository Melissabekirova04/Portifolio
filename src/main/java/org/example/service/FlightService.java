
package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.BadRequestResponse;
import org.example.client.FlightApiClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FlightService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final FlightApiClient flightApiClient = new FlightApiClient();

    public JsonNode searchFlights(
            String origin,
            String destination,
            String departureDate,
            String returnDate,
            int adults,
            boolean directOnly
    ) {
        origin = validateAirportCode(origin);
        destination = validateAirportCode(destination);

        if (origin.equals(destination)) {
            throw new BadRequestResponse(
                    "Afrejse og destination skal være forskellige."
            );
        }

        LocalDate start = parseDate(departureDate);
        LocalDate end = parseDate(returnDate);

        if (start.isBefore(
                LocalDate.now(ZoneId.of("Europe/Copenhagen"))
        )) {
            throw new BadRequestResponse(
                    "Startdatoen må ikke være i fortiden."
            );
        }

        if (!end.isAfter(start)) {
            throw new BadRequestResponse(
                    "Slutdatoen skal ligge efter startdatoen."
            );
        }

        if (adults < 1 || adults > 9) {
            throw new BadRequestResponse(
                    "Vælg mellem 1 og 9 voksne."
            );
        }

        Map<String, Object> data = Map.of(
                "slices", List.of(
                        Map.of(
                                "origin", origin,
                                "destination", destination,
                                "departure_date", start.toString()
                        ),
                        Map.of(
                                "origin", destination,
                                "destination", origin,
                                "departure_date", end.toString()
                        )
                ),
                "passengers", Collections.nCopies(
                        adults,
                        Map.of("type", "adult")
                ),
                "cabin_class", "economy",
                "max_connections", directOnly ? 0 : 2
        );

        String body;

        try {
            body = mapper.writeValueAsString(
                    Map.of("data", data)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke oprette flyforespørgslen.",
                    e
            );
        }

        return flightApiClient.searchOffers(body, origin, destination);
    }

    private String validateAirportCode(String value) {
        if (value == null) {
            throw new BadRequestResponse(
                    "Lufthavnskode mangler."
            );
        }

        String code = value.trim().toUpperCase(Locale.ROOT);

        if (!code.matches("[A-Z]{3}")) {
            throw new BadRequestResponse(
                    "Lufthavnskoden skal bestå af tre bogstaver."
            );
        }

        return code;
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
                    "Datoen er ugyldig."
            );
        }
    }
}
