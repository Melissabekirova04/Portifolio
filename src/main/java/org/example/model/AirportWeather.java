package org.example.model;

public record AirportWeather(
        String city,
        String country,
        String airportCode,
        double temperature,
        double windSpeed,
        int weatherCode,
        String condition
) {
}