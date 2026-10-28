
package org.example.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "trip_flights")
public class TripFlight {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    @Column(name = "offer_id")
    private String offerId;

    @Column(name = "origin")
    private String origin;

    @Column(name = "destination")
    private String destination;

    @Column(name = "departure_date")
    private String departureDate;

    @Column(name = "return_date")
    private String returnDate;

    @Column(name = "airline")
    private String airline;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "flight_json", columnDefinition = "text")
    private String flightJson;

    protected TripFlight() {
        // Bruges af Hibernate.
    }

    public TripFlight(Trip trip, String flightJson) {
        if (trip == null) {
            throw new IllegalArgumentException(
                    "Flyvalget skal tilhøre en rejse."
            );
        }

        this.trip = trip;
        setFlightData(flightJson);
    }

    private void setFlightData(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(
                    "Flytilbuddet må ikke være tomt."
            );
        }

        try {
            JsonNode offer = MAPPER.readTree(json);

            if (offer == null || !offer.isObject()) {
                throw new IllegalArgumentException(
                        "Flytilbuddet skal være et JSON-objekt."
                );
            }

            this.flightJson = json;

            this.offerId = textOrNull(offer, "id");
            this.currency = textOrNull(offer, "total_currency");

            String amount = textOrNull(offer, "total_amount");

            this.totalAmount = amount == null
                    ? null
                    : new BigDecimal(amount);

            JsonNode slices = offer.path("slices");

            this.origin = null;
            this.destination = null;
            this.departureDate = null;
            this.returnDate = null;
            this.airline = null;

            if (slices.isArray() && !slices.isEmpty()) {
                JsonNode outbound = slices.get(0);

                this.origin = airportCode(
                        outbound.path("origin")
                );

                this.destination = airportCode(
                        outbound.path("destination")
                );

                this.departureDate = departureDate(outbound);

                if (slices.size() > 1) {
                    this.returnDate = departureDate(slices.get(1));
                }

                JsonNode segments = outbound.path("segments");

                if (segments.isArray() && !segments.isEmpty()) {
                    JsonNode carrier = segments.get(0)
                            .path("operating_carrier");

                    this.airline = textOrNull(carrier, "name");

                    if (this.airline == null) {
                        this.airline = textOrNull(
                                segments.get(0).path("marketing_carrier"),
                                "name"
                        );
                    }
                }
            }

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Flytilbuddet indeholder ugyldigt JSON.",
                    e
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Flytilbuddet indeholder en ugyldig pris.",
                    e
            );
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);

        if (value.isMissingNode()
                || value.isNull()
                || !value.isValueNode()) {
            return null;
        }

        String text = value.asText();

        return text.isBlank() ? null : text;
    }

    private static String airportCode(JsonNode airport) {
        if (airport.isTextual()) {
            return airport.asText();
        }

        return textOrNull(airport, "iata_code");
    }

    private static String departureDate(JsonNode slice) {
        JsonNode segments = slice.path("segments");

        if (!segments.isArray() || segments.isEmpty()) {
            return null;
        }

        String departingAt = textOrNull(
                segments.get(0),
                "departing_at"
        );

        if (departingAt == null || departingAt.length() < 10) {
            return null;
        }

        return departingAt.substring(0, 10);
    }

    public void updateFrom(TripFlight selected) {
        if (selected == null) {
            throw new IllegalArgumentException(
                    "Flyvalget mangler."
            );
        }

        setFlightData(selected.flightJson);
    }

    public void removeTrip() {
        this.trip = null;
    }

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public String getAirline() {
        return airline;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getFlightJson() {
        return flightJson;
    }
}
