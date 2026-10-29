
package org.example.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(
            name = "destination_json",
            nullable = false,
            columnDefinition = "text"
    )
    private String destinationJson;

    @Column(name = "search_json", columnDefinition = "text")
    private String searchJson;

    @Column(name = "flight_json", columnDefinition = "text")
    private String flightJson;

    @Column(name = "hotel_json", columnDefinition = "text")
    private String hotelJson;

    @Column(name = "price_summary_json", columnDefinition = "text")
    private String priceSummaryJson;

    @Column(name = "activities_json", columnDefinition = "text")
    private String activitiesJson;

    @OneToMany(
            mappedBy = "trip",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("date ASC, time ASC, id ASC")
    private List<Activity> activities = new ArrayList<>();

    @OneToOne(
            mappedBy = "trip",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private TripBudget budget;

    @OneToOne(
            mappedBy = "trip",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private TripHotel hotel;

    @OneToOne(
            mappedBy = "trip",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private TripFlight flight;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Trip() {
    }

    public Trip(
            User user,
            String destinationJson,
            String searchJson,
            String flightJson,
            String hotelJson,
            String priceSummaryJson
    ) {
        this.user = user;
        this.destinationJson = destinationJson;
        this.searchJson = searchJson;
        this.flightJson = flightJson;
        this.hotelJson = hotelJson;
        this.priceSummaryJson = priceSummaryJson;
        this.activitiesJson = "[]";
        this.createdAt = Instant.now();
    }

    public void addActivity(Activity activity) {
        Objects.requireNonNull(
                activity,
                "Aktiviteten må ikke være null."
        );

        if (activity.getTrip() != null
                && activity.getTrip() != this) {
            throw new IllegalArgumentException(
                    "Aktiviteten tilhører allerede en anden rejse."
            );
        }

        if (!activities.contains(activity)) {
            activities.add(activity);
        }

        activity.assignTrip(this);
    }

    public void removeActivity(Activity activity) {
        if (activities.remove(activity)) {
            activity.assignTrip(null);
        }
    }

    public void updateFlight(
            String flightJson,
            String priceSummaryJson
    ) {
        this.flightJson = flightJson;
        this.priceSummaryJson = priceSummaryJson;
    }

    public void updateFlightSnapshot(String flightJson) {
        this.flightJson = flightJson;
    }

    public void changeFlight(TripFlight selected) {
        if (selected == null) {
            if (flight != null) {
                flight.removeTrip();
                flight = null;
            }
            return;
        }

        if (selected.getTrip() != this) {
            throw new IllegalArgumentException(
                    "Flyvalget skal tilhøre denne rejse."
            );
        }

        if (flight == selected) {
            return;
        }

        if (flight == null) {
            flight = selected;
        } else {
            flight.updateFrom(selected);
        }
    }

    public void updateHotel(
            String hotelJson,
            String priceSummaryJson
    ) {
        this.hotelJson = hotelJson;
        this.priceSummaryJson = priceSummaryJson;
    }

    public void updateHotelSnapshot(String hotelJson) {
        this.hotelJson = hotelJson;
    }

    public void changeHotel(TripHotel selected) {
        if (selected == null) {
            if (hotel != null) {
                hotel.removeTrip();
                hotel = null;
            }
            return;
        }

        if (selected.getTrip() != this) {
            throw new IllegalArgumentException(
                    "Hotelvalget skal tilhøre denne rejse."
            );
        }

        if (hotel == selected) {
            return;
        }

        if (hotel == null) {
            hotel = selected;
        } else {
            hotel.updateFrom(selected);
        }
    }

    public void updateActivities(String activitiesJson) {
        this.activitiesJson = activitiesJson;
    }

    public void updateBudget(BigDecimal amount) {
        if (amount == null) {
            if (budget != null) {
                budget.removeTrip();
                budget = null;
            }
            return;
        }

        if (budget == null) {
            budget = new TripBudget(this, amount);
        } else {
            budget.updateAmount(amount);
        }
    }

    public Long getId() {
        return id;
    }

    public String getDestinationJson() {
        return destinationJson;
    }

    public String getSearchJson() {
        return searchJson;
    }

    public String getFlightJson() {
        return flightJson;
    }

    public String getHotelJson() {
        return hotelJson;
    }

    public String getPriceSummaryJson() {
        return priceSummaryJson;
    }

    public String getActivitiesJson() {
        return activitiesJson;
    }

    public List<Activity> getActivities() {
        return Collections.unmodifiableList(activities);
    }

    public TripBudget getBudget() {
        return budget;
    }

    public TripHotel getHotel() {
        return hotel;
    }

    public TripFlight getFlight() {
        return flight;
    }

    public BigDecimal getBudgetAmount() {
        if (budget == null) {
            return null;
        }

        return budget.getMaxAmount();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
