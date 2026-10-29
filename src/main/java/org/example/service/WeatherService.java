package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.BadGatewayResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.ServiceUnavailableResponse;
import org.example.client.WeatherApiClient;
import org.example.model.AirportCity;
import org.example.model.AirportWeather;
import org.example.service.ExperienceService.Experience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WeatherService {

    private static final Logger logger =
            LoggerFactory.getLogger(WeatherService.class);

    private final WeatherApiClient weatherApiClient =
            new WeatherApiClient();

    private final ExperienceService experienceService =
            new ExperienceService();

    private final ObjectMapper mapper = new ObjectMapper();

    private record ClimateCache(
            ObjectNode data,
            Instant expiresAt
    ) {}

    private final Map<String, ClimateCache> climateCache =
            new ConcurrentHashMap<>();

    private final List<AirportCity> airports = List.of(
            // Danmark
            new AirportCity(
                    "København", "Danmark", "CPH", 55.6181, 12.6561
            ),
            new AirportCity(
                    "Aarhus", "Danmark", "AAR", 56.3000, 10.6190
            ),
            new AirportCity(
                    "Aalborg", "Danmark", "AAL", 57.0928, 9.8492
            ),
            new AirportCity(
                    "Billund", "Danmark", "BLL", 55.7403, 9.1518
            ),

            // Norge
            new AirportCity(
                    "Oslo", "Norge", "OSL", 60.1939, 11.1004
            ),
            new AirportCity(
                    "Bergen", "Norge", "BGO", 60.2934, 5.2181
            ),
            new AirportCity(
                    "Trondheim", "Norge", "TRD", 63.4578, 10.9240
            ),
            new AirportCity(
                    "Stavanger", "Norge", "SVG", 58.8767, 5.6378
            ),

            // Sverige
            new AirportCity(
                    "Stockholm", "Sverige", "ARN", 59.6519, 17.9186
            ),
            new AirportCity(
                    "Göteborg", "Sverige", "GOT", 57.6628, 12.2798
            ),
            new AirportCity(
                    "Malmö", "Sverige", "MMX", 55.5363, 13.3762
            ),
            new AirportCity(
                    "Umeå", "Sverige", "UME", 63.7918, 20.2828
            )
    );

    private static final Set<String> INDOOR = Set.of(
            "AAR-aros",
            "AAR-moesgaard",
            "AAL-utzon",
            "AAL-kunsten",
            "BLL-lego-house",
            "OSL-munch",
            "TRD-rockheim",
            "SVG-oliemuseum",
            "SVG-kunstmuseum",
            "ARN-vasa",
            "GOT-universeum",
            "MMX-malmoehus",
            "UME-bildmuseet",
            "UME-vasterbotten"
    );

    private static final Set<String> MIXED = Set.of(
            "AAR-den-gamle-by",
            "TRD-nidaros",
            "ARN-skansen"
    );

    public List<AirportWeather> getAllAirportWeather() {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        List<Callable<AirportWeather>> tasks = new ArrayList<>();

        for (AirportCity airport : airports) {
            Callable<AirportWeather> task = () ->
                    getWeatherByAirportCode(airport.airportCode());

            tasks.add(task);
        }

        logger.info(
                "Henter vejr for {} destinationer med op til fire tråde.",
                airports.size()
        );

        try {

            List<Future<AirportWeather>> futures =
                    executor.invokeAll(tasks, 70, TimeUnit.SECONDS);

            List<AirportWeather> results = new ArrayList<>();

            for (Future<AirportWeather> future : futures) {
                results.add(future.get());
            }

            logger.info(
                    "Vejr hentet for {} destinationer.",
                    results.size()
            );

            return results;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            logger.warn("Hentning af vejr blev afbrudt.");

            throw new ServiceUnavailableResponse(
                    "Hentning af vejr blev afbrudt. Prøv igen."
            );

        } catch (CancellationException e) {
            logger.warn("Den samlede tidsgrænse for vejrkald blev nået.");

            throw new ServiceUnavailableResponse(
                    "Det tog for lang tid at hente alle vejrdata. "
                            + "Prøv igen senere."
            );

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            logger.warn("En vejropgave fejlede.", cause);


            if (cause instanceof ServiceUnavailableResponse) {
                throw new ServiceUnavailableResponse(
                        cause.getMessage()
                );
            }

            if (cause instanceof BadGatewayResponse) {
                throw new BadGatewayResponse(
                        cause.getMessage()
                );
            }

            throw new BadGatewayResponse(
                    "Kunne ikke hente vejret for alle destinationer."
            );

        } finally {

            executor.shutdownNow();
        }
    }


    public AirportWeather getWeatherByAirportCode(String code) {
        AirportCity airport = requireAirport(code);

        JsonNode current = weatherApiClient.getWeather(
                airport.latitude(),
                airport.longitude()
        ).path("current");

        int weatherCode = (int) requiredNumber(
                current.path("weather_code")
        );

        return new AirportWeather(
                airport.city(),
                airport.country(),
                airport.airportCode(),
                requiredNumber(current.path("temperature_2m")),
                requiredNumber(current.path("wind_speed_10m")),
                weatherCode,
                convertWeatherCode(weatherCode)
        );
    }


    public ObjectNode getOverview(String code) {
        AirportCity airport = requireAirport(code);

        JsonNode data = weatherApiClient.getWeather(
                airport.latitude(),
                airport.longitude()
        );

        ObjectNode response = mapper.createObjectNode();

        response.put("destination", airport.airportCode());
        response.put("city", airport.city());
        response.put("country", airport.country());
        response.put("source", "Open-Meteo");
        response.put("sourceUrl", "https://open-meteo.com/");
        response.put("timezone", data.path("timezone").asText());
        response.put("temperatureUnit", "°C");
        response.put("windSpeedUnit", "km/h");
        response.put("precipitationUnit", "mm");

        response.put(
                "locationNotice",
                "Vejr for destinationsområdet med lufthavnen "
                        + "som geografisk reference."
        );

        ObjectNode current = data.path("current").deepCopy();
        JsonNode currentCode = current.path("weather_code");

        current.put(
                "condition",
                currentCode.isNumber()
                        ? convertWeatherCode(currentCode.asInt())
                        : "Vejrforhold ikke tilgængelige"
        );

        response.set("current", current);

        JsonNode daily = data.path("daily");
        ArrayNode week = mapper.createArrayNode();

        for (int i = 0; i < daily.path("time").size(); i++) {
            ObjectNode day = mapper.createObjectNode();

            day.set("date", daily.path("time").get(i));

            copyDaily(day, "temperatureMin", daily,
                    "temperature_2m_min", i);
            copyDaily(day, "temperatureMax", daily,
                    "temperature_2m_max", i);
            copyDaily(day, "precipitationMm", daily,
                    "precipitation_sum", i);
            copyDaily(day, "precipitationProbability", daily,
                    "precipitation_probability_max", i);
            copyDaily(day, "windSpeedMax", daily,
                    "wind_speed_10m_max", i);
            copyDaily(day, "weatherCode", daily,
                    "weather_code", i);

            JsonNode weatherCode = day.path("weatherCode");

            day.put(
                    "condition",
                    weatherCode.isNumber()
                            ? convertWeatherCode(weatherCode.asInt())
                            : "Vejrforhold ikke tilgængelige"
            );

            week.add(day);
        }

        response.set("week", week);

        response.put(
                "climateUrl",
                "/api/weather/destinations/"
                        + airport.airportCode() + "/climate"
        );

        return response;
    }


    public ObjectNode getRecommendations(
            String code,
            LocalDate requestedDate
    ) {
        AirportCity airport = requireAirport(code);
        ObjectNode overview = getOverview(code);

        LocalDate date = requestedDate != null
                ? requestedDate
                : LocalDate.now(
                ZoneId.of(overview.path("timezone").asText())
        );

        JsonNode forecast = mapper.nullNode();

        for (JsonNode day : overview.path("week")) {
            if (date.toString().equals(day.path("date").asText())) {
                forecast = day;
                break;
            }
        }

        String preference = "neutral";

        String reason =
                "Datoen ligger uden for den viste prognose. "
                        + "Forslagene prioriteres ikke efter vejret.";

        if (!forecast.isNull()) {
            JsonNode weatherCode = forecast.path("weatherCode");
            JsonNode precipitation = forecast.path("precipitationMm");
            JsonNode probability =
                    forecast.path("precipitationProbability");
            JsonNode wind = forecast.path("windSpeedMax");
            JsonNode temperature = forecast.path("temperatureMax");

            boolean complete = weatherCode.isNumber()
                    && precipitation.isNumber()
                    && probability.isNumber()
                    && wind.isNumber()
                    && temperature.isNumber();

            if (complete) {
                int wmo = weatherCode.asInt();

                boolean wetOrSnow = wmo >= 51 && wmo <= 86;
                boolean thunder = wmo == 95 || wmo == 96 || wmo == 99;
                boolean fog = wmo == 45 || wmo == 48;

                boolean preferIndoor = wetOrSnow
                        || thunder
                        || fog
                        || precipitation.asDouble() >= 1
                        || probability.asDouble() >= 50
                        || wind.asDouble() >= 40
                        || temperature.asDouble() < 5
                        || temperature.asDouble() > 30;

                if (preferIndoor) {
                    preference = "indoor";

                    reason =
                            "Indendørs oplevelser prioriteres på grund "
                                    + "af nedbør, tåge, vind eller temperatur.";

                } else if (wmo >= 0 && wmo <= 2) {
                    preference = "outdoor";

                    reason =
                            "Udendørs oplevelser prioriteres, fordi "
                                    + "prognosen viser forhold, der passer "
                                    + "til udendørs aktiviteter.";

                } else {
                    reason = "Ingen særlig prioritering "
                            + "ud fra dagens prognose.";
                }

            } else {
                reason = "Prognosen mangler oplysninger. "
                        + "Forslagene prioriteres ikke efter vejret.";
            }
        }

        final String selectedPreference = preference;

        List<Experience> experiences = new ArrayList<>(
                experienceService.findByDestination(
                        airport.airportCode()
                )
        );

        experiences.sort(
                Comparator.comparingInt(experience ->
                        priority(experience, selectedPreference))
        );

        ArrayNode suggestions = mapper.createArrayNode();

        for (Experience experience : experiences) {
            ObjectNode item = mapper.valueToTree(experience);

            item.put("environment", environment(experience));
            item.put(
                    "weatherPriority",
                    priority(experience, selectedPreference)
            );
            item.put(
                    "detailsUrl",
                    "/api/experiences/" + experience.id()
            );

            suggestions.add(item);
        }

        ObjectNode response = mapper.createObjectNode();

        response.put("destination", airport.airportCode());
        response.put("date", date.toString());
        response.put("forecastAvailable", !forecast.isNull());
        response.set("forecast", forecast);
        response.put("preferredEnvironment", preference);
        response.put("reason", reason);
        response.set("experiences", suggestions);

        response.put(
                "notice",
                "Vejret ændrer kun rækkefølgen. "
                        + "Det ændrer ikke din gemte rejse. "
                        + "Åbningstider og ledige billetter kontrolleres ikke."
        );

        return response;
    }


    public ObjectNode getClimate(String code) {
        AirportCity airport = requireAirport(code);
        String key = airport.airportCode();

        ClimateCache cached = climateCache.get(key);

        if (cached != null
                && cached.expiresAt().isAfter(Instant.now())) {
            return cached.data().deepCopy();
        }

        JsonNode data = weatherApiClient.getHistoricalWeather(
                airport.latitude(),
                airport.longitude()
        );

        JsonNode daily = data.path("daily");

        JsonNode dates = daily.path("time");
        JsonNode temperatures = daily.path("temperature_2m_mean");
        JsonNode precipitation = daily.path("precipitation_sum");

        LocalDate start = LocalDate.of(1991, 1, 1);
        LocalDate end = LocalDate.of(2020, 12, 31);

        int expectedDays = (int) ChronoUnit.DAYS.between(
                start, end
        ) + 1;

        if (!dates.isArray()
                || !temperatures.isArray()
                || !precipitation.isArray()
                || dates.size() != expectedDays
                || temperatures.size() != expectedDays
                || precipitation.size() != expectedDays) {
            throw new BadGatewayResponse(
                    "Klimadataene dækker ikke hele perioden."
            );
        }

        double[][] temperatureSum = new double[30][12];
        double[][] precipitationSum = new double[30][12];
        int[][] dayCounts = new int[30][12];

        for (int i = 0; i < expectedDays; i++) {
            LocalDate date = start.plusDays(i);

            if (!date.toString().equals(dates.get(i).asText())) {
                throw new BadGatewayResponse(
                        "Klimadataene indeholder manglende datoer."
                );
            }

            int yearIndex = date.getYear() - 1991;
            int monthIndex = date.getMonthValue() - 1;

            temperatureSum[yearIndex][monthIndex] +=
                    requiredNumber(temperatures.get(i));

            precipitationSum[yearIndex][monthIndex] +=
                    requiredNumber(precipitation.get(i));

            dayCounts[yearIndex][monthIndex]++;
        }

        ArrayNode months = mapper.createArrayNode();

        for (int month = 1; month <= 12; month++) {
            double meanTemperatureSum = 0;
            double monthlyPrecipitationSum = 0;

            for (int yearIndex = 0; yearIndex < 30; yearIndex++) {
                int expectedCount = YearMonth.of(
                        1991 + yearIndex, month
                ).lengthOfMonth();

                if (dayCounts[yearIndex][month - 1] != expectedCount) {
                    throw new BadGatewayResponse(
                            "Der mangler dage i klimaberegningen."
                    );
                }

                meanTemperatureSum +=
                        temperatureSum[yearIndex][month - 1]
                                / expectedCount;

                monthlyPrecipitationSum +=
                        precipitationSum[yearIndex][month - 1];
            }

            ObjectNode item = mapper.createObjectNode();

            item.put("month", month);

            item.put(
                    "name",
                    Month.of(month).getDisplayName(
                            TextStyle.FULL,
                            Locale.forLanguageTag("da-DK")
                    )
            );

            item.put(
                    "averageTemperature",
                    round(meanTemperatureSum / 30)
            );

            item.put(
                    "averageMonthlyPrecipitationMm",
                    round(monthlyPrecipitationSum / 30)
            );

            months.add(item);
        }

        ObjectNode response = mapper.createObjectNode();

        response.put("destination", key);
        response.put("city", airport.city());
        response.put("source", "Open-Meteo / ERA5");

        response.put(
                "sourceUrl",
                "https://open-meteo.com/en/docs/historical-weather-api"
        );

        response.put("referencePeriod", "1991-2020");
        response.put("temperatureUnit", "°C");
        response.put("precipitationUnit", "mm");
        response.put("isForecast", false);
        response.put("calculatedAt", Instant.now().toString());

        response.put(
                "notice",
                "Beregnet fra historiske modeldata for 1991-2020. "
                        + "Temperatur er gennemsnit af månedernes "
                        + "døgnmiddeltemperaturer. Nedbør er gennemsnitlig "
                        + "samlet nedbør pr. måned. "
                        + "Dette er ikke en vejrudsigt."
        );

        response.set("months", months);

        climateCache.put(
                key,
                new ClimateCache(
                        response.deepCopy(),
                        Instant.now().plus(Duration.ofHours(24))
                )
        );

        return response;
    }

    private AirportCity requireAirport(String code) {
        return airports.stream()
                .filter(airport ->
                        airport.airportCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new NotFoundResponse(
                        "Destinationen findes ikke: " + code
                ));
    }

    private String environment(Experience experience) {
        if (INDOOR.contains(experience.id())) {
            return "indoor";
        }

        if (MIXED.contains(experience.id())) {
            return "mixed";
        }

        return "outdoor";
    }

    private int priority(
            Experience experience,
            String preference
    ) {
        if ("neutral".equals(preference)) {
            return 0;
        }

        String environment = environment(experience);

        if (environment.equals(preference)) {
            return 0;
        }

        return "mixed".equals(environment) ? 1 : 2;
    }

    private void copyDaily(
            ObjectNode target,
            String targetName,
            JsonNode daily,
            String sourceName,
            int index
    ) {
        JsonNode value = daily.path(sourceName).path(index);

        target.set(
                targetName,
                value.isMissingNode() ? mapper.nullNode() : value
        );
    }

    private double requiredNumber(JsonNode value) {
        if (value == null
                || !value.isNumber()
                || !Double.isFinite(value.asDouble())) {
            throw new BadGatewayResponse(
                    "Vejrtjenesten mangler nødvendige målinger."
            );
        }

        return value.asDouble();
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private String convertWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Klart vejr";
            case 1 -> "Overvejende klart";
            case 2 -> "Delvist overskyet";
            case 3 -> "Overskyet";
            case 45, 48 -> "Tåge";
            case 51, 53, 55 -> "Støvregn";
            case 56, 57 -> "Underafkølet støvregn";
            case 61, 63, 65 -> "Regn";
            case 66, 67 -> "Underafkølet regn";
            case 71, 73, 75, 77 -> "Sne";
            case 80, 81, 82 -> "Regnbyger";
            case 85, 86 -> "Snebyger";
            case 95 -> "Torden";
            case 96, 99 -> "Torden med hagl";
            default -> "Ukendt vejr";
        };
    }
}