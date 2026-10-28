
package org.example.dao;

import org.example.entity.Trip;
import org.example.entity.TripFlight;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TripFlightTest {

    @Test
    void extractsFlightDetailsFromDuffelJson() throws Exception {

        String flightJson = """
                {
                  "id": "off_test123",
                  "total_amount": "1850.00",
                  "total_currency": "DKK",
                  "slices": [
                    {
                      "origin": {
                        "iata_code": "CPH"
                      },
                      "destination": {
                        "iata_code": "OSL"
                      },
                      "segments": [
                        {
                          "departing_at": "2026-11-20T10:30:00",
                          "operating_carrier": {
                            "name": "SAS"
                          }
                        }
                      ]
                    },
                    {
                      "origin": {
                        "iata_code": "OSL"
                      },
                      "destination": {
                        "iata_code": "CPH"
                      },
                      "segments": [
                        {
                          "departing_at": "2026-11-25T15:00:00"
                        }
                      ]
                    }
                  ]
                }
                """;

        // Opretter Trip via dens Hibernate-constructor.
        Constructor<Trip> constructor =
                Trip.class.getDeclaredConstructor();

        constructor.setAccessible(true);

        Trip trip = constructor.newInstance();

        // Opretter flyvalg ud fra Duffel JSON.
        TripFlight flight = new TripFlight(trip, flightJson);

        // Kontrollerer relationen til rejsen.
        assertSame(trip, flight.getTrip());

        // Kontrollerer flyoplysningerne.
        assertEquals("off_test123", flight.getOfferId());
        assertEquals("CPH", flight.getOrigin());
        assertEquals("OSL", flight.getDestination());

        assertEquals(
                "2026-11-20",
                flight.getDepartureDate()
        );

        assertEquals(
                "2026-11-25",
                flight.getReturnDate()
        );

        assertEquals("SAS", flight.getAirline());

        assertEquals(
                new BigDecimal("1850.00"),
                flight.getTotalAmount()
        );

        assertEquals("DKK", flight.getCurrency());

        // Kontrollerer, at hele JSON-indholdet bevares.
        assertEquals(flightJson, flight.getFlightJson());
    }
}
