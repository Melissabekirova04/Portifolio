
package org.example.config;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.Activity;
import org.example.entity.Trip;
import org.example.entity.TripBudget;
import org.example.entity.TripFlight;
import org.example.entity.TripHotel;
import org.example.entity.User;
import org.hibernate.cfg.Configuration;

public class HibernateConfig {

    private static EntityManagerFactory emf;

    private HibernateConfig() {
    }

    public static synchronized EntityManagerFactory
    getEntityManagerFactory() {

        if (emf == null) {
            String url = requireEnvironmentVariable("DB_URL");

            emf = buildFactory(url, "update");
        }

        return emf;
    }

    public static synchronized EntityManagerFactory
    getTestEntityManagerFactory() {

        if (emf != null) {
            throw new IllegalStateException(
                    "Testdatabasen skal vælges, før DAO'er oprettes."
            );
        }

        emf = buildFactory(
                "jdbc:postgresql://localhost:5432/travelplanner_test",
                "create-drop"
        );

        return emf;
    }

    private static EntityManagerFactory buildFactory(
            String url,
            String schemaMode
    ) {
        String username = requireEnvironmentVariable("DB_USERNAME");
        String password = requireEnvironmentVariable("DB_PASSWORD");

        return new Configuration()
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Trip.class)
                .addAnnotatedClass(Activity.class)
                .addAnnotatedClass(TripBudget.class)
                .addAnnotatedClass(TripHotel.class)
                .addAnnotatedClass(TripFlight.class)
                .setProperty(
                        "hibernate.connection.driver_class",
                        "org.postgresql.Driver"
                )
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", username)
                .setProperty("hibernate.connection.password", password)
                .setProperty(
                        "hibernate.dialect",
                        "org.hibernate.dialect.PostgreSQLDialect"
                )
                .setProperty("hibernate.hbm2ddl.auto", schemaMode)
                .setProperty("hibernate.show_sql", "false")
                .buildSessionFactory();
    }

    private static String requireEnvironmentVariable(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    name + " skal være sat."
            );
        }

        return value;
    }

    public static synchronized void close() {
        if (emf != null) {
            if (emf.isOpen()) {
                emf.close();
            }

            emf = null;
        }
    }
}
