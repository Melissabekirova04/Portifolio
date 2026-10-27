package org.example.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_hotels")
public class TripHotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    @Column(name = "hotel_id", nullable = false)
    private String hotelId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private String city;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int adults;

    @Column(nullable = false)
    private int rooms;

    @Column(
            name = "nightly_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal pricePerRoomPerNight;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "test_data", nullable = false)
    private boolean testData;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    private String address;

    @Column(name = "location_description", columnDefinition = "text")
    private String locationDescription;

    @ElementCollection
    @CollectionTable(
            name = "trip_hotel_amenities",
            joinColumns = @JoinColumn(name = "trip_hotel_id")
    )
    @OrderColumn(name = "sort_order")
    @Column(name = "amenity", nullable = false)
    private List<String> amenities = new ArrayList<>();

    protected TripHotel() {
        // Bruges af Hibernate.
    }

    public TripHotel(
            Trip trip,
            String hotelId,
            String name,
            String destination,
            String city,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int rooms,
            BigDecimal pricePerRoomPerNight,
            BigDecimal totalAmount,
            String currency,
            boolean testData,
            BigDecimal rating,
            String address,
            String locationDescription,
            List<String> amenities
    ) {
        this.trip = trip;
        this.hotelId = hotelId;
        this.name = name;
        this.destination = destination;
        this.city = city;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.adults = adults;
        this.rooms = rooms;
        this.pricePerRoomPerNight = pricePerRoomPerNight;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.testData = testData;
        this.rating = rating;
        this.address = address;
        this.locationDescription = locationDescription;
        this.amenities = new ArrayList<>(amenities);
    }

    // Opdater det eksisterende hotelvalg.
    void updateFrom(TripHotel selected) {
        this.hotelId = selected.hotelId;
        this.name = selected.name;
        this.destination = selected.destination;
        this.city = selected.city;
        this.checkIn = selected.checkIn;
        this.checkOut = selected.checkOut;
        this.adults = selected.adults;
        this.rooms = selected.rooms;
        this.pricePerRoomPerNight = selected.pricePerRoomPerNight;
        this.totalAmount = selected.totalAmount;
        this.currency = selected.currency;
        this.testData = selected.testData;
        this.rating = selected.rating;
        this.address = selected.address;
        this.locationDescription = selected.locationDescription;

        this.amenities.clear();
        this.amenities.addAll(selected.amenities);
    }

    // Bruges af Trip, når hotelvalget fjernes.
    void removeTrip() {
        this.trip = null;
    }

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public String getHotelId() {
        return hotelId;
    }

    public String getName() {
        return name;
    }

    public String getDestination() {
        return destination;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getAdults() {
        return adults;
    }

    public int getRooms() {
        return rooms;
    }

    public BigDecimal getPricePerRoomPerNight() {
        return pricePerRoomPerNight;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isTestData() {
        return testData;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public String getAddress() {
        return address;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public List<String> getAmenities() {
        return List.copyOf(amenities);
    }
}