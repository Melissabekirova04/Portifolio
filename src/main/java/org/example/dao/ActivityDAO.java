package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.config.HibernateConfig;
import org.example.entity.Activity;
import org.example.entity.Trip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ActivityDAO {

    private final EntityManagerFactory emf;

    public ActivityDAO() {
        this.emf = HibernateConfig.getEntityManagerFactory();
    }

    public Activity create(
            Long tripId,
            Long userId,
            Activity activity
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            Trip trip = findOwnedTrip(em, tripId, userId);

            trip.addActivity(activity);
            em.persist(activity);

            em.getTransaction().commit();

            return activity;

        } catch (RuntimeException e) {
            rollback(em);
            throw e;
        } finally {
            em.close();
        }
    }


    public List<Activity> findAll(Long tripId, Long userId) {
        EntityManager em = emf.createEntityManager();

        try {
            findOwnedTrip(em, tripId, userId);

            return em.createQuery("""
                    SELECT a
                    FROM Activity a
                    WHERE a.trip.id = :tripId
                      AND a.trip.user.id = :userId
                    ORDER BY a.date, a.time, a.id
                    """, Activity.class)
                    .setParameter("tripId", tripId)
                    .setParameter("userId", userId)
                    .getResultList();

        } finally {
            em.close();
        }
    }


    public Activity findById(
            String activityId,
            Long tripId,
            Long userId
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            return findOwnedActivity(
                    em, activityId, tripId, userId
            );
        } finally {
            em.close();
        }
    }


    public Activity update(
            String activityId,
            Long tripId,
            Long userId,
            String name,
            LocalDate date,
            LocalTime time,
            BigDecimal priceAmount,
            String currency,
            String category,
            String notes
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            Activity activity = findOwnedActivity(
                    em, activityId, tripId, userId
            );

            activity.updateDetails(
                    name,
                    date,
                    time,
                    priceAmount,
                    currency,
                    category,
                    notes
            );

            em.getTransaction().commit();

            return activity;

        } catch (RuntimeException e) {
            rollback(em);
            throw e;
        } finally {
            em.close();
        }
    }


    public void delete(
            String activityId,
            Long tripId,
            Long userId
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            Activity activity = findOwnedActivity(
                    em, activityId, tripId, userId
            );

            Trip trip = activity.getTrip();
            trip.removeActivity(activity);

            em.getTransaction().commit();

        } catch (RuntimeException e) {
            rollback(em);
            throw e;
        } finally {
            em.close();
        }
    }


    private Trip findOwnedTrip(
            EntityManager em,
            Long tripId,
            Long userId
    ) {
        List<Trip> trips = em.createQuery("""
                SELECT t
                FROM Trip t
                WHERE t.id = :tripId
                  AND t.user.id = :userId
                """, Trip.class)
                .setParameter("tripId", tripId)
                .setParameter("userId", userId)
                .getResultList();

        if (trips.isEmpty()) {
            throw new IllegalArgumentException(
                    "Rejsen findes ikke."
            );
        }

        return trips.get(0);
    }

    private Activity findOwnedActivity(
            EntityManager em,
            String activityId,
            Long tripId,
            Long userId
    ) {
        List<Activity> activities = em.createQuery("""
                SELECT a
                FROM Activity a
                WHERE a.id = :activityId
                  AND a.trip.id = :tripId
                  AND a.trip.user.id = :userId
                """, Activity.class)
                .setParameter("activityId", activityId)
                .setParameter("tripId", tripId)
                .setParameter("userId", userId)
                .getResultList();

        if (activities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aktiviteten findes ikke på denne rejse."
            );
        }

        return activities.get(0);
    }

    private void rollback(EntityManager em) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }
}