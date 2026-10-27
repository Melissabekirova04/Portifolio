package org.example.controller;

import io.javalin.Javalin;
import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.config.HibernateConfig;
import org.example.dao.TripDAO;
import org.example.entity.Trip;
import org.example.security.JwtService;
import org.example.security.SecurityMiddleware;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class ActivityApiTest {

    private static EntityManagerFactory emf;
    private static Javalin app;
    private static String baseUrl;

    private static final String PASSWORD = "ApiTest2026!";

    private String token;
    private String otherToken;
    private Long tripId;
    private LocalDate startDate;

    @BeforeAll
    static void startServer() {
        emf = HibernateConfig.getTestEntityManagerFactory();

        JwtService jwtService = new JwtService();

        app = Javalin.create();

        new SecurityMiddleware(jwtService).register(app);
        new AuthController(jwtService).registerRoutes(app);
        new ActivityController().registerRoutes(app);
        new ItineraryController().registerRoutes(app);

        app.start(0);

        baseUrl = "http://localhost:" + app.port();
    }

    @BeforeEach
    void setUp() {
        clearDatabase();

        token = registerAndLogin(
                "Melissa",
                "activity-test@example.com"
        );

        otherToken = registerAndLogin(
                "Anden bruger",
                "other-activity-test@example.com"
        );

        Long userId = given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/auth/me")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("userId");

        startDate = LocalDate.now().plusDays(30);
        LocalDate endDate = startDate.plusDays(5);

        TripDAO tripDAO = new TripDAO(emf);

        Trip trip = tripDAO.create(
                userId,
                """
                {
                  "city":"Oslo",
                  "country":"Norway",
                  "airportCode":"OSL"
                }
                """,
                """
                {
                  "origin":"CPH",
                  "destination":"OSL",
                  "departureDate":"%s",
                  "returnDate":"%s",
                  "adults":1,
                  "directOnly":false
                }
                """.formatted(startDate, endDate),
                null,
                """
                {
                  "id":"OSL-budget",
                  "name":"Oslo Budget Hotel (test)",
                  "destination":"OSL",
                  "city":"Oslo",
                  "checkIn":"%s",
                  "checkOut":"%s",
                  "adults":1,
                  "rooms":1,
                  "nights":5,
                  "pricePerRoomPerNight":825.00,
                  "totalAmount":4125.00,
                  "currency":"DKK",
                  "testData":true,
                  "rating":7.4,
                  "address":"Testvej 10, Oslo",
                  "locationDescription":"Tæt på offentlig transport.",
                  "amenities":["Wi-Fi","Eget badeværelse"]
                }
                """.formatted(startDate, endDate),
                null
        );

        tripId = trip.getId();

        tripDAO.updateOwnedTrip(
                tripId,
                userId,
                savedTrip -> savedTrip.updateBudget(
                        new java.math.BigDecimal("8000.00")
                )
        ).orElseThrow();
    }

    @AfterAll
    static void stopServer() {
        try {
            if (app != null) {
                app.stop();
            }
        } finally {
            HibernateConfig.close();
        }
    }

    @Test
    void validTokenGrantsAccess() {
        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/auth/me")
                .then()
                .statusCode(200);
    }

    @Test
    void wrongPasswordIsRejected() {
        given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(Map.of(
                        "email", "activity-test@example.com",
                        "password", "ForkertKode2026!"
                ))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void missingTokenIsRejected() {
        given()
                .baseUri(baseUrl)
                .when()
                .get(activitiesPath())
                .then()
                .statusCode(401);
    }

    @Test
    void invalidTokenIsRejected() {
        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer ugyldigt-token")
                .when()
                .get(activitiesPath())
                .then()
                .statusCode(401);
    }

    @Test
    void anotherUserCannotAccessOrChangeActivities() {
        String activityId = createActivity();
        String path = activitiesPath() + "/" + activityId;

        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + otherToken)
                .when()
                .get(activitiesPath())
                .then()
                .statusCode(404);

        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + otherToken)
                .contentType("application/json")
                .body(activityInput(
                        "Uønsket ændring",
                        startDate.plusDays(1),
                        999
                ))
                .when()
                .put(path)
                .then()
                .statusCode(404);

        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + otherToken)
                .when()
                .delete(path)
                .then()
                .statusCode(404);

        Response response = getActivities();

        assertEquals(
                "Testaktivitet",
                response.jsonPath().getString("activities[0].name")
        );

        assertEquals(
                100.0,
                response.jsonPath()
                        .getDouble("activities[0].priceAmount"),
                0.01
        );
    }

    @Test
    void dateOutsideTripIsRejected() {
        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(activityInput(
                        "Ugyldig dato",
                        startDate.minusDays(1),
                        100
                ))
                .when()
                .post(activitiesPath() + "/custom")
                .then()
                .statusCode(400);

        assertEquals(
                0,
                getActivities().jsonPath().getInt("count")
        );
    }

    @Test
    void negativePriceIsRejected() {
        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(activityInput(
                        "Ugyldig pris",
                        startDate.plusDays(1),
                        -100
                ))
                .when()
                .post(activitiesPath() + "/custom")
                .then()
                .statusCode(400);

        assertEquals(
                0,
                getActivities().jsonPath().getInt("count")
        );
    }

    @Test
    void activityCrudUpdatesBudget() {
        String activityId = createActivity();

        Response activities = getActivities();

        assertEquals(1, activities.jsonPath().getInt("count"));

        assertEquals(
                activityId,
                activities.jsonPath()
                        .getString("activities[0].id")
        );

        assertBudget(4225.00, 3775.00);

        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(activityInput(
                        "Opdateret aktivitet",
                        startDate.plusDays(2),
                        150
                ))
                .when()
                .put(activitiesPath() + "/" + activityId)
                .then()
                .statusCode(200);

        Response updated = getActivities();

        assertEquals(
                "Opdateret aktivitet",
                updated.jsonPath().getString("activities[0].name")
        );

        assertEquals(
                150.0,
                updated.jsonPath()
                        .getDouble("activities[0].priceAmount"),
                0.01
        );

        assertBudget(4275.00, 3725.00);

        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(activitiesPath() + "/" + activityId)
                .then()
                .statusCode(200);

        assertEquals(
                0,
                getActivities().jsonPath().getInt("count")
        );

        assertBudget(4125.00, 3875.00);
    }

    private String registerAndLogin(String name, String email) {
        given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(Map.of(
                        "name", name,
                        "email", email,
                        "password", PASSWORD
                ))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        return given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", PASSWORD
                ))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    private String createActivity() {
        return given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(activityInput(
                        "Testaktivitet",
                        startDate.plusDays(1),
                        100
                ))
                .when()
                .post(activitiesPath() + "/custom")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("activity.id");
    }

    private Map<String, Object> activityInput(
            String name,
            LocalDate date,
            int price
    ) {
        return Map.of(
                "name", name,
                "date", date.toString(),
                "time", "12:00",
                "priceAmount", price,
                "category", "Test",
                "notes", "Oprettet af en automatiseret test"
        );
    }

    private Response getActivities() {
        return given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(activitiesPath())
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private void assertBudget(
            double expectedTotal,
            double expectedRemaining
    ) {
        Response response = given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/trips/" + tripId + "/itinerary")
                .then()
                .statusCode(200)
                .extract()
                .response();

        assertEquals(
                expectedTotal,
                response.jsonPath().getDouble("budget.totalAmount"),
                0.01
        );

        assertEquals(
                expectedRemaining,
                response.jsonPath().getDouble("budget.remainingBudget"),
                0.01
        );
    }

    private String activitiesPath() {
        return "/api/trips/" + tripId + "/activities";
    }

    private void clearDatabase() {
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

            em.getTransaction().commit();

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw e;
        } finally {
            em.close();
        }
    }
}