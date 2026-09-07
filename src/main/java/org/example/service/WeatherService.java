package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.client.WeatherApiClient;
import org.example.model.AirportCity;
import org.example.model.AirportWeather;

import java.util.List;

public class WeatherService {

    private final WeatherApiClient weatherApiClient = new WeatherApiClient();

    // Lufthavne som Travel Planner understøtter
    private final List<AirportCity> airports = List.of(

            // DANMARK
            new AirportCity("København", "Danmark", "CPH", 55.6181, 12.6561),
            new AirportCity("Billund", "Danmark", "BLL", 55.7403, 9.1518),
            new AirportCity("Aalborg", "Danmark", "AAL", 57.0928, 9.8492),
            new AirportCity("Aarhus", "Danmark", "AAR", 56.3000, 10.6190),

            // SVERIGE
            new AirportCity("Stockholm", "Sverige", "ARN", 59.6519, 17.9186),
            new AirportCity("Göteborg", "Sverige", "GOT", 57.6628, 12.2798),
            new AirportCity("Malmö", "Sverige", "MMX", 55.5363, 13.3762),

            // NORGE
            new AirportCity("Oslo", "Norge", "OSL", 60.1939, 11.1004),
            new AirportCity("Bergen", "Norge", "BGO", 60.2934, 5.2181),
            new AirportCity("Stavanger", "Norge", "SVG", 58.8767, 5.6378),
            new AirportCity("Trondheim", "Norge", "TRD", 63.4578, 10.9240),
            new AirportCity("Tromsø", "Norge", "TOS", 69.6833, 18.9189)
    );

    // Henter vejret for alle lufthavne
    public List<AirportWeather> getAllAirportWeather() {
        return airports.stream()
                .map(this::getWeatherForAirport)
                .toList();
    }

    // Henter vejret for én bestemt lufthavn
    public AirportWeather getWeatherByAirportCode(String airportCode) {

        AirportCity airport = airports.stream()
                .filter(a -> a.airportCode().equalsIgnoreCase(airportCode))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lufthavn ikke fundet: " + airportCode
                        )
                );

        return getWeatherForAirport(airport);
    }

    // Kalder vores eksterne API
    private AirportWeather getWeatherForAirport(AirportCity airport) {

        JsonNode response = weatherApiClient.getWeather(
                airport.latitude(),
                airport.longitude()
        );

        JsonNode current = response.get("current");

        double temperature =
                current.get("temperature_2m").asDouble();

        double windSpeed =
                current.get("wind_speed_10m").asDouble();

        int weatherCode =
                current.get("weather_code").asInt();

        return new AirportWeather(
                airport.city(),
                airport.country(),
                airport.airportCode(),
                temperature,
                windSpeed,
                weatherCode,
                convertWeatherCode(weatherCode)
        );
    }

    // Gør API'ets weather codes nemmere at forstå
    private String convertWeatherCode(int code) {

        return switch (code) {
            case 0 -> "Klart vejr";
            case 1, 2, 3 -> "Delvist overskyet";
            case 45, 48 -> "Tåge";
            case 51, 53, 55 -> "Støvregn";
            case 61, 63, 65 -> "Regn";
            case 71, 73, 75 -> "Sne";
            case 80, 81, 82 -> "Regnbyger";
            case 95, 96, 99 -> "Torden";
            default -> "Ukendt vejr";
        };
    }
}