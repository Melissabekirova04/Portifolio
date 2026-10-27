package org.example.controller;

import com.password4j.Password;
import io.javalin.Javalin;
import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.config.HibernateConfig;
import org.example.dao.UserDAO;
import org.example.entity.Trip;
import org.example.entity.User;
import org.example.security.JwtService;
import org.example.security.SecurityMiddleware;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class AuthApiTest {

    private static EntityManagerFactory emf;
    private static Javalin app;
    private static String baseUrl;

    private static final String EMAIL = "auth-test@example.com";
    private static final String PASSWORD = "TestRejse2026!";

    @BeforeAll
    static void startServer() {
        // Vælg testdatabasen, før controllerens DAO oprettes.
        emf = HibernateConfig.getTestEntityManagerFactory();

        JwtService jwtService = new JwtService();

        app = Javalin.create();

        new SecurityMiddleware(jwtService).register(app);
        new AuthController(jwtService).registerRoutes(app);

        // 0 betyder, at serveren vælger en ledig port.
        app.start(0);

        baseUrl = "http://localhost:" + app.port();
    }

    @BeforeEach
    void clearDatabase() {
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
    void registersUserAndHashesPassword() {
        Response response = register(
                "  AUTH-TEST@EXAMPLE.COM  ",
                PASSWORD
        );

        assertEquals(201, response.statusCode());
        assertEquals(EMAIL, response.jsonPath().getString("email"));
        assertEquals("Melissa", response.jsonPath().getString("name"));

        // Adgangskoden skal ikke være med i svaret.
        assertFalse(response.jsonPath().getMap("$")
                .containsKey("password"));
        assertFalse(response.jsonPath().getMap("$")
                .containsKey("passwordHash"));

        User saved = new UserDAO(emf)
                .findByEmail(EMAIL)
                .orElseThrow();

        assertNotEquals(PASSWORD, saved.getPasswordHash());

        assertTrue(
                Password.check(PASSWORD, saved.getPasswordHash())
                        .withBcrypt()
        );
    }

    @Test
    void rejectsDuplicateEmail() {
        assertEquals(201, register(EMAIL, PASSWORD).statusCode());

        Response response = register(
                "AUTH-TEST@EXAMPLE.COM",
                PASSWORD
        );

        assertEquals(409, response.statusCode());
        assertEquals(
                "E-mailen er allerede registreret",
                response.jsonPath().getString("error")
        );
    }

    @Test
    void rejectsShortPassword() {
        Response response = register(EMAIL, "kort");

        assertEquals(400, response.statusCode());

        assertTrue(
                new UserDAO(emf).findByEmail(EMAIL).isEmpty()
        );
    }

    @Test
    void loginReturnsTokenThatGrantsAccess() {
        assertEquals(201, register(EMAIL, PASSWORD).statusCode());

        Response login = login(PASSWORD);

        assertEquals(200, login.statusCode());
        assertEquals("no-store", login.header("Cache-Control"));
        assertEquals(
                "Bearer",
                login.jsonPath().getString("tokenType")
        );

        String token = login.jsonPath().getString("token");

        assertNotNull(token);
        assertFalse(token.isBlank());

        Response me = given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/auth/me");

        assertEquals(200, me.statusCode());

        Long expectedUserId = new UserDAO(emf)
                .findByEmail(EMAIL)
                .orElseThrow()
                .getId();

        assertEquals(
                expectedUserId,
                me.jsonPath().getLong("userId")
        );
    }

    @Test
    void rejectsWrongPassword() {
        assertEquals(201, register(EMAIL, PASSWORD).statusCode());

        Response response = login("ForkertKode2026!");

        assertEquals(401, response.statusCode());
        assertEquals(
                "Forkert e-mail eller adgangskode",
                response.jsonPath().getString("error")
        );

        assertFalse(
                response.jsonPath().getMap("$").containsKey("token")
        );
    }

    @Test
    void rejectsAccessWithoutToken() {
        Response response = given()
                .baseUri(baseUrl)
                .when()
                .get("/api/auth/me");

        assertEquals(401, response.statusCode());
    }

    private Response register(String email, String password) {
        return given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(Map.of(
                        "name", "Melissa",
                        "email", email,
                        "password", password
                ))
                .when()
                .post("/api/auth/register");
    }

    private Response login(String password) {
        return given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .body(Map.of(
                        "email", EMAIL,
                        "password", password
                ))
                .when()
                .post("/api/auth/login");
    }
}