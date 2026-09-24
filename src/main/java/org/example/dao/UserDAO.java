package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.config.HibernateConfig;
import org.example.entity.User;

import java.util.Optional;

public class UserDAO {

    private final EntityManagerFactory emf =
            HibernateConfig.getEntityManagerFactory();

    public User create(User user) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return user;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<User> findByEmail(String email) {
        EntityManager em = emf.createEntityManager();

        try {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.email = :email",
                            User.class
                    )
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }
}