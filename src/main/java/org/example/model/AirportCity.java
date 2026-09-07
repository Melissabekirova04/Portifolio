package org.example.model;

public record AirportCity(
        String city,
        String country,
        String airportCode,
        double latitude,
        double longitude
) {
}