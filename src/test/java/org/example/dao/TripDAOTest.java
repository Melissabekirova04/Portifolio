package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.entity.Activity;
import org.example.entity.Trip;
import org.example.entity.TripBudget;
import org.example.entity.TripHotel;
import org.example.entity.User;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TripDAOTest {

    private static EntityManagerFactory emf;

    private TripDAO tripDAO;
    private Long melissaId;
    private Long otherUserId;

    @BeforeAll
    static void setUpDatabase() {
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if (username == null || password == null) {
            throw new IllegalStateException(
                    "Testen mangler DB_USERNAME og DB_PASSWORD."
            );
        }

        emf = new Configuration()
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Trip.class)
                .addAnnotatedClass(Activity.class)
                .addAnnotatedClass(TripBudget.class)
                .addAnnotatedClass(TripHotel.class)
                .setProperty(
                        "hibernate.connection.driver_class",
                        "org.postgresql.Driver"
                )
                .setProperty(
                        "hibernate.connection.url",
                        "jdbc:postgresql://localhost:5432/travelplanner_test"
                )
                .setProperty(
                        "hibernate.connection.username",
                        username
                )
                .setProperty(
                        "hibernate.connection.password",
                        password
                )
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false")
                .buildSessionFactory();
    }

    @BeforeEach
    void setUp() {
        tripDAO = new TripDAO(emf);

        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            // Kun testdatabasen bruges her.
            // remove bruger relationernes cascade til oprydning.
            List<Trip> trips = em.createQuery(
                    "SELECT t FROM Trip t",
                    Trip.class
            ).getResultList();

            for (Trip trip : trips) {
                em.remove(trip);
            }

            // Udfør sletningerne, før brugerne slettes.
            em.flush();

            em.createQuery("DELETE FROM User").executeUpdate();

            // Password-hashing testes ikke i denne DAO-test.
            User melissa = new User(
                    "Melissa",
                    "melissa-test@example.com",
                    "test-hash"
            );

            User otherUser = new User(
                    "Anden bruger",
                    "other-test@example.com",
                    "test-hash"
            );

            em.persist(melissa);
            em.persist(otherUser);

            em.getTransaction().commit();

            melissaId = melissa.getId();
            otherUserId = otherUser.getId();

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
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void createsAndReadsTrip() {
        Trip created = createTrip(melissaId);

        Optional<Trip> result = tripDAO.findByIdAndUserId(
                created.getId(),
                melissaId
        );

        assertNotNull(created.getId());
        assertTrue(result.isPresent());

        assertEquals(
                created.getDestinationJson(),
                result.get().getDestinationJson()
        );

        assertEquals(
                created.getSearchJson(),
                result.get().getSearchJson()
        );
    }

    @Test
    void onlyFindsTripsOwnedByUser() {
        Trip melissasTrip = createTrip(melissaId);
        createTrip(otherUserId);

        List<Trip> trips = tripDAO.findByUserId(melissaId);

        assertEquals(1, trips.size());
        assertEquals(melissasTrip.getId(), trips.get(0).getId());
    }

    @Test
    void cannotReadAnotherUsersTrip() {
        Trip trip = createTrip(melissaId);

        Optional<Trip> result = tripDAO.findByIdAndUserId(
                trip.getId(),
                otherUserId
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void savesAndUpdatesBudget() {
        Trip trip = createTrip(melissaId);

        tripDAO.updateOwnedTrip(
                trip.getId(),
                melissaId,
                savedTrip -> savedTrip.updateBudget(
                        new BigDecimal("8000")
                )
        ).orElseThrow();

        Trip firstRead = tripDAO.findByIdAndUserId(
                trip.getId(),
                melissaId
        ).orElseThrow();

        assertEquals(
                new BigDecimal("8000.00"),
                firstRead.getBudgetAmount()
        );

        tripDAO.updateOwnedTrip(
                trip.getId(),
                melissaId,
                savedTrip -> savedTrip.updateBudget(
                        new BigDecimal("9000")
                )
        ).orElseThrow();

        Trip secondRead = tripDAO.findByIdAndUserId(
                trip.getId(),
                melissaId
        ).orElseThrow();

        assertEquals(
                new BigDecimal("9000.00"),
                secondRead.getBudgetAmount()
        );

        assertEquals(1L, countBudgets());
    }

    @Test
    void cannotUpdateAnotherUsersTrip() {
        Trip trip = createTrip(melissaId);

        Optional<Trip> result = tripDAO.updateOwnedTrip(
                trip.getId(),
                otherUserId,
                savedTrip -> savedTrip.updateBudget(
                        new BigDecimal("1000")
                )
        );

        assertTrue(result.isEmpty());

        Trip unchanged = tripDAO.findByIdAndUserId(
                trip.getId(),
                melissaId
        ).orElseThrow();

        assertNull(unchanged.getBudgetAmount());
        assertEquals(0L, countBudgets());
    }

    @Test
    void removesBudgetButKeepsTrip() {
        Trip trip = createTrip(melissaId);

        tripDAO.updateOwnedTrip(
                trip.getId(),
                melissaId,
                savedTrip -> savedTrip.updateBudget(
                        new BigDecimal("8000")
                )
        ).orElseThrow();

        assertEquals(1L, countBudgets());

        tripDAO.updateOwnedTrip(
                trip.getId(),
                melissaId,
                savedTrip -> savedTrip.updateBudget(null)
        ).orElseThrow();

        Trip saved = tripDAO.findByIdAndUserId(
                trip.getId(),
                melissaId
        ).orElseThrow();

        assertNull(saved.getBudgetAmount());
        assertEquals(0L, countBudgets());
    }

    @Test
    void rollsBackWhenUpdateFails() {
        Trip trip = createTrip(melissaId);

        assertThrows(
                IllegalStateException.class,
                () -> tripDAO.updateOwnedTrip(
                        trip.getId(),
                        melissaId,
                        savedTrip -> {
                            savedTrip.updateBudget(
                                    new BigDecimal("8000")
                            );

                            throw new IllegalStateException(
                                    "Simuleret fejl"
                            );
                        }
                )
        );

        Trip saved = tripDAO.findByIdAndUserId(
                trip.getId(),
                melissaId
        ).orElseThrow();

        assertNull(saved.getBudgetAmount());
        assertEquals(0L, countBudgets());
    }

    private Trip createTrip(Long userId) {
        return tripDAO.create(
                userId,
                """
                {"city":"Oslo","country":"Norway","airportCode":"OSL"}
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
                null,
                null
        );
    }

    private long countBudgets() {
        EntityManager em = emf.createEntityManager();

        try {
            return em.createQuery(
                    "SELECT COUNT(b) FROM TripBudget b",
                    Long.class
            ).getSingleResult();
        } finally {
            em.close();
        }
    }
}