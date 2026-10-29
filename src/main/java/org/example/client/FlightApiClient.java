
package org.example.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.BadGatewayResponse;
import io.javalin.http.ServiceUnavailableResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;

public class FlightApiClient {

    private static final Logger logger =
            LoggerFactory.getLogger(FlightApiClient.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JsonNode searchOffers(String body, String origin, String destination) {

        String token = System.getenv("DUFFEL_TOKEN");

        if (token == null || token.isBlank()) {
            logger.error("DUFFEL_TOKEN mangler i serverens opsætning.");

            throw new ServiceUnavailableResponse(
                    "Flysøgningen er ikke tilgængelig lige nu."
            );
        }

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

        logger.info(
                "Starter flysøgning fra {} til {}.",
                origin,
                destination
        );

        Instant startedAt = Instant.now();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();

            if (status == 429) {
                logger.warn("Duffel afviste flysøgningen med HTTP 429.");

                throw new ServiceUnavailableResponse(
                        "Der er sendt for mange flysøgninger. Prøv igen senere."
                );
            }

            if (status < 200 || status >= 300) {
                logger.warn(
                        "Duffel afviste flysøgningen med HTTP {}.",
                        status
                );

                throw new BadGatewayResponse(
                        "Flysøgningen kunne ikke gennemføres. Prøv igen senere."
                );
            }

            JsonNode responseData = mapper.readTree(response.body());

            if (responseData == null
                    || !responseData.isObject()
                    || !responseData.path("data").isObject()
                    || !responseData.path("data").path("offers").isArray()) {

                logger.warn("Duffel returnerede et uventet svarformat.");

                throw new BadGatewayResponse(
                        "Flysøgningen returnerede et uventet svar."
                );
            }

            JsonNode result = responseData.path("data");

            logger.info(
                    "Flysøgning afsluttet med {} tilbud efter {} ms.",
                    result.path("offers").size(),
                    Duration.between(startedAt, Instant.now()).toMillis()
            );

            return result;

        } catch (HttpTimeoutException e) {
            logger.warn("Flysøgningen overskred tidsgrænsen.");

            throw new ServiceUnavailableResponse(
                    "Flysøgningen tog for lang tid. Prøv igen senere."
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            logger.warn("Flysøgningen blev afbrudt.");

            throw new ServiceUnavailableResponse(
                    "Flysøgningen blev afbrudt. Prøv igen senere."
            );

        } catch (JsonProcessingException e) {
            logger.warn("Duffel returnerede ugyldigt JSON.");

            throw new BadGatewayResponse(
                    "Flysøgningen returnerede ugyldige data."
            );

        } catch (IOException e) {
            logger.warn("Forbindelsen til Duffel fejlede.");

            throw new BadGatewayResponse(
                    "Kunne ikke kontakte flytjenesten. Prøv igen senere."
            );
        }
    }
}
