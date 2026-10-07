package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.config.HibernateConfig;
import org.example.entity.Trip;
import org.example.entity.User;

import java.util.List;
import java.util.Optional;

public class TripDAO {

    private final EntityManagerFactory emf =
            HibernateConfig.getEntityManagerFactory();

    public Trip create(
            Long userId,
            String destinationJson,
            String searchJson,
            String flightJson
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            User user = em.find(User.class, userId);

            if (user == null) {
                throw new IllegalArgumentException("Brugeren findes ikke");
            }

            Trip trip = new Trip(
                    user,
                    destinationJson,
                    searchJson,
                    flightJson
            );

            em.persist(trip);
            em.getTransaction().commit();

            return trip;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw e;
        } finally {
            em.close();
        }
    }

    public List<Trip> findByUserId(Long userId) {
        EntityManager em = emf.createEntityManager();

        try {
            return em.createQuery("""
                    SELECT t
                    FROM Trip t
                    WHERE t.user.id = :userId
                    ORDER BY t.createdAt DESC, t.id DESC
                    """, Trip.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Optional<Trip> findByIdAndUserId(Long tripId, Long userId) {
        EntityManager em = emf.createEntityManager();

        try {
            return em.createQuery("""
                    SELECT t
                    FROM Trip t
                    WHERE t.id = :tripId
                      AND t.user.id = :userId
                    """, Trip.class)
                    .setParameter("tripId", tripId)
                    .setParameter("userId", userId)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }
}