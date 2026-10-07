package org.example.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "destination_json", nullable = false, columnDefinition = "text")
    private String destinationJson;

    @Column(name = "search_json", columnDefinition = "text")
    private String searchJson;

    @Column(name = "flight_json", columnDefinition = "text")
    private String flightJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Trip() {
        // Hibernate
    }

    public Trip(
            User user,
            String destinationJson,
            String searchJson,
            String flightJson
    ) {
        this.user = user;
        this.destinationJson = destinationJson;
        this.searchJson = searchJson;
        this.flightJson = flightJson;
        this.createdAt = Instant.now();
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}