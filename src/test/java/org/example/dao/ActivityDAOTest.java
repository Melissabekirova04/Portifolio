package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.config.HibernateConfig;
import org.example.entity.Activity;
import org.example.entity.Trip;
import org.example.entity.User;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActivityDAOTest {

    private static EntityManagerFactory emf;

    private ActivityDAO activityDAO;
    private Long userId;
    private Long otherUserId;
    private Long tripId;

    @BeforeAll
    static void setUpDatabase() {
        emf = HibernateConfig.getTestEntityManagerFactory();
    }

    @BeforeEach
    void setUp() {
        activityDAO = new ActivityDAO();

        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            // Fjern testrejser og deres tilknyttede data.
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
                    "melissa-test@example.com",
                    "test-hash"
            );

            User otherUser = new User(
                    "Anden bruger",
                    "other-test@example.com",
                    "test-hash"
            );

            em.persist(user);
            em.persist(otherUser);

            Trip trip = new Trip(
                    user,
                    """
                    {"city":"Oslo","airportCode":"OSL"}
                    """,
                    null,
                    null,
                    null,
                    null
            );

            em.persist(trip);

            em.getTransaction().commit();

            userId = user.getId();
            otherUserId = otherUser.getId();
            tripId = trip.getId();

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
    void createsAndReadsActivity() {
        Activity created = activityDAO.create(
                tripId,
                userId,
                newActivity(
                        "Museumsbesøg",
                        LocalDate.of(2026, 11, 21),
                        LocalTime.of(10, 30)
                )
        );

        Activity saved = activityDAO.findById(
                created.getId(),
                tripId,
                userId
        );

        assertEquals(created.getId(), saved.getId());
        assertEquals("Museumsbesøg", saved.getName());
        assertEquals(
                LocalDate.of(2026, 11, 21),
                saved.getDate()
        );
        assertEquals(LocalTime.of(10, 30), saved.getTime());
        assertEquals(
                new BigDecimal("100.00"),
                saved.getPriceAmount()
        );
        assertEquals("custom", saved.getType());
        assertNotNull(saved.getAddedAt());
    }

    @Test
    void sortsActivitiesByDateAndTime() {
        Activity laterDay = activityDAO.create(
                tripId,
                userId,
                newActivity(
                        "Næste dag",
                        LocalDate.of(2026, 11, 22),
                        LocalTime.of(9, 0)
                )
        );

        Activity afternoon = activityDAO.create(
                tripId,
                userId,
                newActivity(
                        "Eftermiddag",
                        LocalDate.of(2026, 11, 21),
                        LocalTime.of(15, 0)
                )
        );

        Activity morning = activityDAO.create(
                tripId,
                userId,
                newActivity(
                        "Formiddag",
                        LocalDate.of(2026, 11, 21),
                        LocalTime.of(10, 0)
                )
        );

        List<Activity> activities = activityDAO.findAll(
                tripId,
                userId
        );

        assertEquals(
                List.of(
                        morning.getId(),
                        afternoon.getId(),
                        laterDay.getId()
                ),
                activities.stream()
                        .map(Activity::getId)
                        .toList()
        );
    }

    @Test
    void updatesActivity() {
        Activity created = activityDAO.create(
                tripId,
                userId,
                newActivity(
                        "Oprindelig aktivitet",
                        LocalDate.of(2026, 11, 21),
                        LocalTime.of(10, 0)
                )
        );

        activityDAO.update(
                created.getId(),
                tripId,
                userId,
                "Besøg på museum",
                LocalDate.of(2026, 11, 22),
                LocalTime.of(14, 30),
                new BigDecimal("150.00"),
                "DKK",
                "Kultur",
                "Husk billet"
        );

        // Hent igen for at kontrollere de gemte ændringer.
        Activity saved = activityDAO.findById(
                created.getId(),
                tripId,
                userId
        );

        assertEquals("Besøg på museum", saved.getName());
        assertEquals(
                LocalDate.of(2026, 11, 22),
                saved.getDate()
        );
        assertEquals(LocalTime.of(14, 30), saved.getTime());
        assertEquals(
                new BigDecimal("150.00"),
                saved.getPriceAmount()
        );
        assertEquals("DKK", saved.getCurrency());
        assertEquals("Kultur", saved.getCategory());
        assertEquals("Husk billet", saved.getNotes());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(
                created.getAddedAt().truncatedTo(
                        java.time.temporal.ChronoUnit.SECONDS
                ),
                saved.getAddedAt().truncatedTo(
                        java.time.temporal.ChronoUnit.SECONDS
                )
        );
    }

    @Test
    void deletesActivityButKeepsTrip() {
        Activity created = activityDAO.create(
                tripId,
                userId,
                newActivity(
                        "Aktivitet som slettes",
                        LocalDate.of(2026, 11, 21),
                        LocalTime.of(10, 0)
                )
        );

        activityDAO.delete(
                created.getId(),
                tripId,
                userId
        );

        assertTrue(
                activityDAO.findAll(tripId, userId).isEmpty()
        );

        TripDAO tripDAO = new TripDAO(emf);

        assertTrue(
                tripDAO.findByIdAndUserId(
                        tripId,
                        userId
                ).isPresent()
        );
    }

    @Test
    void rejectsAnotherUsersAccess() {
        Activity created = activityDAO.create(
                tripId,
                userId,
                newActivity(
                        "Privat aktivitet",
                        LocalDate.of(2026, 11, 21),
                        LocalTime.of(10, 0)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> activityDAO.findById(
                        created.getId(),
                        tripId,
                        otherUserId
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> activityDAO.delete(
                        created.getId(),
                        tripId,
                        otherUserId
                )
        );

        // Ejerens aktivitet skal stadig findes.
        Activity saved = activityDAO.findById(
                created.getId(),
                tripId,
                userId
        );

        assertEquals("Privat aktivitet", saved.getName());
    }

    private Activity newActivity(
            String name,
            LocalDate date,
            LocalTime time
    ) {
        return new Activity(
                name,
                date,
                time,
                new BigDecimal("100.00"),
                "DKK",
                "Test",
                "En testaktivitet",
                "custom",
                null,
                "OSL",
                null
        );
    }
}