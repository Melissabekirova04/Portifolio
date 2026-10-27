package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.config.HibernateConfig;
import org.example.entity.Trip;
import org.example.entity.TripHotel;
import org.example.entity.User;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TripHotelTest {

    private static EntityManagerFactory emf;

    private TripDAO tripDAO;
    private Long userId;

    @BeforeAll
    static void setUpDatabase() {
        emf = HibernateConfig.getTestEntityManagerFactory();
    }

    @BeforeEach
    void setUp() {
        tripDAO = new TripDAO(emf);

        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            List<Trip> trips = em.createQuery(
                    "SELECT t FROM Trip t",
                    Trip.class
            ).getResultList();

            for (Trip trip : trips) {
                em.remove(trip);
            }

            em.flush();

            em.createQuery("DELETE FROM User").executeUpdate();

            User user = new User(
                    "Melissa",
                    "hotel-test@example.com",
                    "test-hash"
            );

            em.persist(user);

            em.getTransaction().commit();

            userId = user.getId();

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw e;
        } finally {
            em.close();
        }
    }

    @AfterAll
    static void closeDatabase() {
        HibernateConfig.close();
    }

    @Test
    void savesHotelWithTrip() {
        Trip created = createTripWithHotel();

        Trip saved = findTrip(created.getId());
        TripHotel hotel = saved.getHotel();

        assertNotNull(hotel);
        assertNotNull(hotel.getId());
        assertEquals("OSL-budget", hotel.getHotelId());
        assertEquals("Oslo Budget Hotel (test)", hotel.getName());
        assertEquals(
                new BigDecimal("4125.00"),
                hotel.getTotalAmount()
        );
        assertEquals(
                List.of("Wi-Fi", "Eget badeværelse"),
                hotel.getAmenities()
        );

        assertEquals(1L, countHotels());
    }

    @Test
    void updatesExistingHotelRow() {
        Trip created = createTripWithHotel();

        Long originalHotelId = findTrip(created.getId())
                .getHotel()
                .getId();

        tripDAO.updateOwnedTrip(
                created.getId(),
                userId,
                trip -> trip.updateHotel(
                        hotelJson(
                                "OSL-city",
                                "Oslo City Hotel (test)",
                                "1125.00",
                                "5625.00"
                        ),
                        null
                )
        ).orElseThrow();

        TripHotel hotel = findTrip(created.getId()).getHotel();

        assertEquals(originalHotelId, hotel.getId());
        assertEquals("OSL-city", hotel.getHotelId());
        assertEquals("Oslo City Hotel (test)", hotel.getName());
        assertEquals(
                new BigDecimal("5625.00"),
                hotel.getTotalAmount()
        );

        assertEquals(1L, countHotels());
    }

    @Test
    void removesHotelButKeepsTrip() {
        Trip created = createTripWithHotel();

        tripDAO.updateOwnedTrip(
                created.getId(),
                userId,
                trip -> trip.updateHotel(null, null)
        ).orElseThrow();

        Trip saved = findTrip(created.getId());

        assertEquals(created.getId(), saved.getId());
        assertNull(saved.getHotel());
        assertNull(saved.getHotelJson());
        assertEquals(0L, countHotels());
        assertEquals(0L, countAmenities());
    }

    private Trip createTripWithHotel() {
        return tripDAO.create(
                userId,
                """
                {"city":"Oslo","airportCode":"OSL"}
                """,
                """
                {
                  "origin":"CPH",
                  "destination":"OSL",
                  "departureDate":"2026-11-20",
                  "returnDate":"2026-11-25",
                  "adults":1,
                  "directOnly":false
                }
                """,
                null,
                hotelJson(
                        "OSL-budget",
                        "Oslo Budget Hotel (test)",
                        "825.00",
                        "4125.00"
                ),
                null
        );
    }

    private Trip findTrip(Long tripId) {
        return tripDAO.findByIdAndUserId(
                tripId,
                userId
        ).orElseThrow();
    }

    private String hotelJson(
            String hotelId,
            String name,
            String nightlyPrice,
            String totalAmount
    ) {
        return """
                {
                  "id":"%s",
                  "name":"%s",
                  "destination":"OSL",
                  "city":"Oslo",
                  "checkIn":"2026-11-20",
                  "checkOut":"2026-11-25",
                  "adults":1,
                  "rooms":1,
                  "nights":5,
                  "pricePerRoomPerNight":%s,
                  "totalAmount":%s,
                  "currency":"DKK",
                  "testData":true,
                  "rating":7.4,
                  "address":"Testvej 10, Oslo",
                  "locationDescription":"Tæt på offentlig transport.",
                  "amenities":["Wi-Fi","Eget badeværelse"]
                }
                """.formatted(
                hotelId,
                name,
                nightlyPrice,
                totalAmount
        );
    }

    private long countHotels() {
        EntityManager em = emf.createEntityManager();

        try {
            return em.createQuery(
                    "SELECT COUNT(h) FROM TripHotel h",
                    Long.class
            ).getSingleResult();
        } finally {
            em.close();
        }
    }

    private long countAmenities() {
        EntityManager em = emf.createEntityManager();

        try {
            return em.createQuery(
                    """
                    SELECT COUNT(a)
                    FROM TripHotel h
                    JOIN h.amenities a
                    """,
                    Long.class
            ).getSingleResult();
        } finally {
            em.close();
        }
    }
}