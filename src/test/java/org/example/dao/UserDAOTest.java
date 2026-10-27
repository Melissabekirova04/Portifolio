package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.entity.User;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {

    private static EntityManagerFactory emf;

    private UserDAO userDAO;

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
                .setProperty(
                        "hibernate.connection.driver_class",
                        "org.postgresql.Driver"
                )
                .setProperty(
                        "hibernate.connection.url",
                        "jdbc:postgresql://localhost:5432/travelplanner_test"
                )
                .setProperty("hibernate.connection.username", username)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false")
                .buildSessionFactory();
    }

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO(emf);

        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

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
    static void closeDatabase() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void createsAndFindsUserByEmail() {
        User created = userDAO.create(new User(
                "Melissa",
                "melissa-test@example.com",
                "test-hash"
        ));

        Optional<User> result = userDAO.findByEmail(
                "melissa-test@example.com"
        );

        assertNotNull(created.getId());
        assertTrue(result.isPresent());

        User found = result.orElseThrow();

        assertEquals(created.getId(), found.getId());
        assertEquals("Melissa", found.getName());
        assertEquals("melissa-test@example.com", found.getEmail());
        assertEquals("test-hash", found.getPasswordHash());
    }

    @Test
    void returnsEmptyWhenEmailDoesNotExist() {
        userDAO.create(new User(
                "Melissa",
                "melissa-test@example.com",
                "test-hash"
        ));

        Optional<User> result = userDAO.findByEmail(
                "unknown@example.com"
        );

        assertTrue(result.isEmpty());
    }
}