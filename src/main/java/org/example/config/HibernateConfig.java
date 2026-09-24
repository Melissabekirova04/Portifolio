package org.example.config;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.User;
import org.hibernate.cfg.Configuration;

public class HibernateConfig {

    private static EntityManagerFactory emf;

    private HibernateConfig() {
        // Klassen skal ikke oprettes med new
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            String url = System.getenv("DB_URL");
            String username = System.getenv("DB_USERNAME");
            String password = System.getenv("DB_PASSWORD");

            if (url == null || username == null || password == null) {
                throw new IllegalStateException(
                        "DB_URL, DB_USERNAME og DB_PASSWORD skal være sat"
                );
            }

            emf = new Configuration()
                    .addAnnotatedClass(User.class)
                    .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.connection.username", username)
                    .setProperty("hibernate.connection.password", password)
                    .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                    .setProperty("hibernate.hbm2ddl.auto", "update")
                    .setProperty("hibernate.show_sql", "true")
                    .buildSessionFactory();
        }

        return emf;
    }

    public static synchronized void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
        }
    }
}