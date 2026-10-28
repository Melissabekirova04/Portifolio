
package org.example.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.example.config.HibernateConfig;
import org.example.entity.Trip;
import org.example.entity.TripFlight;
import org.example.entity.TripHotel;
import org.example.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class TripDAO {

    private final EntityManagerFactory emf;
    private final ObjectMapper mapper = new ObjectMapper();

    public TripDAO() {
        this(HibernateConfig.getEntityManagerFactory());
    }

    public TripDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Trip create(
            Long userId,
            String destinationJson,
            String searchJson,
            String flightJson,
            String hotelJson,
            String priceSummaryJson
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            User user = em.find(User.class, userId);

            if (user == null) {
                throw new IllegalArgumentException(
                        "Brugeren findes ikke."
                );
            }

            Trip trip = new Trip(
                    user,
                    destinationJson,
                    searchJson,
                    flightJson,
                    hotelJson,
                    priceSummaryJson
            );

            if (hasJsonValue(flightJson)) {
                syncFlightFromJson(trip);
            }

            if (hasJsonValue(hotelJson)) {
                syncHotelFromJson(trip);
            }

            em.persist(trip);
            em.getTransaction().commit();

            return trip;

        } catch (RuntimeException e) {
            rollback(em);
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Trip> findByUserId(Long userId) {
        EntityManager em = emf.createEntityManager();

        try {
            List<Trip> trips = em.createQuery("""
                    SELECT t
                    FROM Trip t
                    LEFT JOIN FETCH t.budget
                    LEFT JOIN FETCH t.hotel
                    LEFT JOIN FETCH t.flight
                    WHERE t.user.id = :userId
                    ORDER BY t.createdAt DESC, t.id DESC
                    """, Trip.class)
                    .setParameter("userId", userId)
                    .getResultList();

            for (Trip trip : trips) {
                initializeRelations(trip);
            }

            return trips;

        } finally {
            em.close();
        }
    }

    public Optional<Trip> findByIdAndUserId(
            Long tripId,
            Long userId
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            Optional<Trip> result = em.createQuery("""
                    SELECT t
                    FROM Trip t
                    LEFT JOIN FETCH t.budget
                    LEFT JOIN FETCH t.hotel
                    LEFT JOIN FETCH t.flight
                    WHERE t.id = :tripId
                      AND t.user.id = :userId
                    """, Trip.class)
                    .setParameter("tripId", tripId)
                    .setParameter("userId", userId)
                    .getResultList()
                    .stream()
                    .findFirst();

            result.ifPresent(this::initializeRelations);

            return result;

        } finally {
            em.close();
        }
    }

    public Optional<Trip> updateOwnedTrip(
            Long tripId,
            Long userId,
            Consumer<Trip> update
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            List<Trip> results = em.createQuery("""
                    SELECT t
                    FROM Trip t
                    WHERE t.id = :tripId
                      AND t.user.id = :userId
                    """, Trip.class)
                    .setParameter("tripId", tripId)
                    .setParameter("userId", userId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();

            if (results.isEmpty()) {
                em.getTransaction().rollback();
                return Optional.empty();
            }

            Trip trip = results.get(0);

            initializeRelations(trip);

            String previousFlightJson = trip.getFlightJson();
            String previousHotelJson = trip.getHotelJson();

            update.accept(trip);

            if (!Objects.equals(
                    previousFlightJson,
                    trip.getFlightJson()
            )) {
                syncFlightFromJson(trip);
            }

            if (!Objects.equals(
                    previousHotelJson,
                    trip.getHotelJson()
            )) {
                syncHotelFromJson(trip);
            }

            initializeRelations(trip);

            em.getTransaction().commit();

            return Optional.of(trip);

        } catch (RuntimeException e) {
            rollback(em);
            throw e;
        } finally {
            em.close();
        }
    }

    private void syncFlightFromJson(Trip trip) {
        String flightJson = trip.getFlightJson();

        if (!hasJsonValue(flightJson)) {
            trip.changeFlight(null);
            return;
        }

        try {
            JsonNode data = mapper.readTree(flightJson);

            if (data == null || data.isNull()) {
                trip.changeFlight(null);
                return;
            }

            if (!data.isObject()) {
                throw new IllegalArgumentException(
                        "Flydata skal være et JSON-objekt."
                );
            }

            // Gem hele flytilbuddet uden at miste detaljer.
            // Vi kan senere udtrække de enkelte felter
            // fra FlightService til separate kolonner.
            TripFlight selected = new TripFlight(
                    trip,
                    flightJson
            );

            trip.changeFlight(selected);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Kunne ikke læse flydata.", e
            );
        }
    }

    private void syncHotelFromJson(Trip trip) {
        String hotelJson = trip.getHotelJson();

        if (!hasJsonValue(hotelJson)) {
            trip.changeHotel(null);
            return;
        }

        try {
            JsonNode data = mapper.readTree(hotelJson);

            if (data == null || data.isNull()) {
                trip.changeHotel(null);
                return;
            }

            if (!data.isObject()) {
                throw new IllegalArgumentException(
                        "Hoteldata skal være et JSON-objekt."
                );
            }

            List<String> amenities = new ArrayList<>();

            for (JsonNode amenity : data.path("amenities")) {
                amenities.add(amenity.asText());
            }

            BigDecimal rating = null;

            if (!data.path("rating").isMissingNode()
                    && !data.path("rating").isNull()) {
                rating = new BigDecimal(
                        data.path("rating").asText()
                );
            }

            TripHotel selected = new TripHotel(
                    trip,
                    data.path("id").asText(),
                    data.path("name").asText(),
                    data.path("destination").asText(),
                    data.path("city").asText(),
                    LocalDate.parse(
                            data.path("checkIn").asText()
                    ),
                    LocalDate.parse(
                            data.path("checkOut").asText()
                    ),
                    data.path("adults").asInt(),
                    data.path("rooms").asInt(),
                    new BigDecimal(
                            data.path("pricePerRoomPerNight").asText()
                    ),
                    new BigDecimal(
                            data.path("totalAmount").asText()
                    ),
                    data.path("currency").asText(),
                    data.path("testData").asBoolean(),
                    rating,
                    optionalText(data, "address"),
                    optionalText(data, "locationDescription"),
                    amenities
            );

            trip.changeHotel(selected);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Kunne ikke læse hoteldata.", e
            );
        }
    }

    private boolean hasJsonValue(String json) {
        return json != null
                && !json.isBlank()
                && !json.trim().equals("null");
    }

    private String optionalText(JsonNode data, String field) {
        JsonNode value = data.path(field);

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private void initializeRelations(Trip trip) {
        trip.getBudgetAmount();

        TripHotel hotel = trip.getHotel();

        if (hotel != null) {
            hotel.getAmenities().size();
        }

        TripFlight flight = trip.getFlight();

        if (flight != null) {
            flight.getFlightJson();
        }
    }

    private void rollback(EntityManager em) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }
}
