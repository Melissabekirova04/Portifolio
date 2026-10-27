package org.example.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "trip_budgets")
public class TripBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "trip_id",
            nullable = false,
            unique = true
    )
    private Trip trip;

    @Column(
            name = "max_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal maxAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    protected TripBudget() {
        // Bruges af Hibernate.
    }

    public TripBudget(Trip trip, BigDecimal maxAmount) {
        this.trip = trip;
        this.currency = "DKK";
        updateAmount(maxAmount);
    }

    public void updateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Budgettet skal være et positivt beløb."
            );
        }

        if (amount.compareTo(
                new BigDecimal("9999999999.99")
        ) > 0) {
            throw new IllegalArgumentException(
                    "Budgettet er for højt."
            );
        }

        try {
            this.maxAmount = amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Budgettet må højst have to decimaler."
            );
        }
    }

    void removeTrip() {
        this.trip = null;
    }

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public String getCurrency() {
        return currency;
    }
}