package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.BadGatewayResponse;
import io.javalin.http.ServiceUnavailableResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FlightService {

    private final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JsonNode searchFlights(
            String origin,
            String destination,
            String departureDate,
            int adults
    ) {
        String token = System.getenv("DUFFEL_TOKEN");

        // Tjekker tokenens format uden at udskrive selve tokenen.
        System.out.println("Token findes: "
                + (token != null && !token.isBlank()));

        System.out.println("Starter med duffel_test_: "
                + (token != null && token.startsWith("duffel_test_")));

        System.out.println("Har mellemrum eller linjeskift: "
                + (token != null
                && token.chars().anyMatch(Character::isWhitespace)));

        if (token == null || token.isBlank()) {
            throw new ServiceUnavailableResponse(
                    "DUFFEL_TOKEN mangler i miljøvariablerne."
            );
        }

        Map<String, Object> data = Map.of(
                "slices", List.of(Map.of(
                        "origin", origin,
                        "destination", destination,
                        "departure_date", departureDate
                )),
                "passengers", Collections.nCopies(
                        adults, Map.of("type", "adult")
                ),
                "cabin_class", "economy"
        );

        try {
            String body = mapper.writeValueAsString(
                    Map.of("data", data)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.duffel.com/air/offer_requests"
                                    + "?return_offers=true"
                                    + "&supplier_timeout=20000"
                    ))
                    .timeout(Duration.ofSeconds(35))
                    .header("Authorization", "Bearer " + token)
                    .header("Duffel-Version", "v2")
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(
                    "Duffel HTTP-status: " + response.statusCode()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new BadGatewayResponse(
                        "Duffel afviste flysøgningen. HTTP "
                                + response.statusCode()
                );
            }

            JsonNode result = mapper.readTree(response.body())
                    .path("data");

            if (!result.path("offers").isArray()) {
                throw new BadGatewayResponse(
                        "Duffel returnerede et uventet svar."
                );
            }

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new BadGatewayResponse(
                    "Flysøgningen blev afbrudt."
            );

        } catch (IOException e) {
            throw new BadGatewayResponse(
                    "Kunne ikke kontakte Duffel."
            );
        }
    }
}