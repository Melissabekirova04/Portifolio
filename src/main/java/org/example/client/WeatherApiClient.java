package org.example.client;

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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherApiClient {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private record CacheEntry(JsonNode data, Instant expiresAt) {}

    private final Map<String, CacheEntry> forecastCache =
            new ConcurrentHashMap<>();

    // Dagens vejr og syv dages prognose.
    public JsonNode getWeather(double latitude, double longitude) {
        String key = latitude + "," + longitude;
        CacheEntry cached = forecastCache.get(key);

        if (cached != null
                && cached.expiresAt().isAfter(Instant.now())) {
            return cached.data().deepCopy();
        }

        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,"
                + "temperature_2m_min,precipitation_sum,"
                + "precipitation_probability_max,wind_speed_10m_max"
                + "&forecast_days=7"
                + "&temperature_unit=celsius"
                + "&wind_speed_unit=kmh"
                + "&precipitation_unit=mm"
                + "&timezone=auto";

        JsonNode data = fetch(url);

        if (!data.path("current").isObject()
                || !data.path("daily").path("time").isArray()) {
            throw new BadGatewayResponse(
                    "Vejr-API'et returnerede et uventet svar."
            );
        }

        forecastCache.put(
                key,
                new CacheEntry(
                        data.deepCopy(),
                        Instant.now().plus(Duration.ofMinutes(10))
                )
        );

        return data;
    }

    // Historiske data til beregning af månedsgennemsnit.
    public JsonNode getHistoricalWeather(
            double latitude,
            double longitude
    ) {
        String url =
                "https://archive-api.open-meteo.com/v1/archive"
                        + "?latitude=" + latitude
                        + "&longitude=" + longitude
                        + "&start_date=1991-01-01"
                        + "&end_date=2020-12-31"
                        + "&daily=temperature_2m_mean,precipitation_sum"
                        + "&models=era5"
                        + "&temperature_unit=celsius"
                        + "&precipitation_unit=mm"
                        + "&timezone=auto";

        JsonNode data = fetch(url);

        if (!data.path("daily").path("time").isArray()) {
            throw new BadGatewayResponse(
                    "Klima-API'et returnerede et uventet svar."
            );
        }

        return data;
    }

    private JsonNode fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 429) {
                throw new ServiceUnavailableResponse(
                        "Vejrtjenestens grænse er nået. Prøv igen senere."
                );
            }

            if (response.statusCode() != 200) {
                throw new BadGatewayResponse(
                        "Vejrtjenesten svarede med HTTP "
                                + response.statusCode() + "."
                );
            }

            JsonNode data = mapper.readTree(response.body());

            if (data == null
                    || !data.isObject()
                    || data.path("error").asBoolean(false)) {
                throw new BadGatewayResponse(
                        "Vejrtjenesten returnerede ugyldige data."
                );
            }

            return data;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new BadGatewayResponse(
                    "Hentning af vejrdata blev afbrudt."
            );

        } catch (IOException e) {
            throw new BadGatewayResponse(
                    "Kunne ikke hente vejrdata. Prøv igen senere."
            );
        }
    }
}