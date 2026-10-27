package org.example.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "activities",
        indexes = {
                @Index(
                        name = "idx_activities_trip_date",
                        columnList = "trip_id, activity_date"
                )
        }
)
public class Activity {

    @Id
    @Column(nullable = false, updatable = false, length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "activity_date", nullable = false)
    private LocalDate date;

    @Column(name = "activity_time", nullable = false)
    private LocalTime time;

    @Column(name = "price_amount", precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(length = 3)
    private String currency;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "experience_id", length = 200)
    private String experienceId;

    @Column(length = 20)
    private String destination;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /*
     * Bevarer ekstra oplysninger fra anbefalede oplevelser,
     * som ikke har deres eget felt i denne entity.
     *
     * Navn, dato, tid, pris og øvrige centrale felter ovenfor
     * bliver de oplysninger, vi søger og opdaterer i databasen.
     */
    @Column(name = "source_json", columnDefinition = "text")
    private String sourceJson;

    protected Activity() {
        // Bruges af Hibernate.
    }

    public Activity(
            String name,
            LocalDate date,
            LocalTime time,
            BigDecimal priceAmount,
            String currency,
            String category,
            String notes,
            String type,
            String experienceId,
            String destination,
            String sourceJson
    ) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.date = date;
        this.time = time;
        this.priceAmount = priceAmount;
        this.currency = currency;
        this.category = category;
        this.notes = notes;
        this.type = type;
        this.experienceId = experienceId;
        this.destination = destination;
        this.sourceJson = sourceJson;
        this.addedAt = Instant.now();
    }

    public void updateDetails(
            String name,
            LocalDate date,
            LocalTime time,
            BigDecimal priceAmount,
            String currency,
            String category,
            String notes
    ) {
        this.name = name;
        this.date = date;
        this.time = time;
        this.priceAmount = priceAmount;
        this.currency = currency;
        this.category = category;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    /*
     * Kun Trip skal ændre relationen, så begge sider
     * kan holdes synkroniserede.
     */
    void assignTrip(Trip trip) {
        this.trip = trip;
    }

    public String getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCategory() {
        return category;
    }

    public String getNotes() {
        return notes;
    }

    public String getType() {
        return type;
    }

    public String getExperienceId() {
        return experienceId;
    }

    public String getDestination() {
        return destination;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getSourceJson() {
        return sourceJson;
    }
}