package org.example.service;

import org.example.service.HotelService.HotelOffer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HotelServiceTest {

    private final HotelService hotelService = new HotelService();

    @Test
    void calculatesPriceForFiveNights() {
        List<HotelOffer> offers = hotelService.search(
                "OSL",
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 25),
                1
        );

        HotelOffer budgetHotel = offers.stream()
                .filter(hotel -> hotel.id().equals("OSL-budget"))
                .findFirst()
                .orElseThrow();

        assertEquals(5L, budgetHotel.nights());
        assertEquals(1, budgetHotel.rooms());

        assertEquals(
                new BigDecimal("4125.00"),
                budgetHotel.totalAmount()
        );

        assertEquals("DKK", budgetHotel.currency());
    }

    @Test
    void threeAdultsRequireTwoRooms() {
        List<HotelOffer> offers = hotelService.search(
                "OSL",
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 25),
                3
        );

        HotelOffer budgetHotel = offers.stream()
                .filter(hotel -> hotel.id().equals("OSL-budget"))
                .findFirst()
                .orElseThrow();

        assertEquals(2, budgetHotel.rooms());

        assertEquals(
                new BigDecimal("8250.00"),
                budgetHotel.totalAmount()
        );
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() {
        assertThrows(
                IllegalArgumentException.class,
                () -> hotelService.search(
                        "OSL",
                        LocalDate.of(2026, 11, 25),
                        LocalDate.of(2026, 11, 20),
                        1
                )
        );
    }

    @Test
    void rejectsUnknownDestination() {
        assertThrows(
                IllegalArgumentException.class,
                () -> hotelService.search(
                        "XXX",
                        LocalDate.of(2026, 11, 20),
                        LocalDate.of(2026, 11, 25),
                        1
                )
        );
    }
}