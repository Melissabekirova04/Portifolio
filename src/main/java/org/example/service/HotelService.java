package org.example.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class HotelService {

    private record City(String name, int basePrice) {}

    public record HotelOffer(
            String id,
            String name,
            String destination,
            String city,
            String checkIn,
            String checkOut,
            int adults,
            int rooms,
            long nights,
            BigDecimal pricePerRoomPerNight,
            BigDecimal totalAmount,
            String currency,
            boolean testData,
            BigDecimal rating,
            String address,
            String locationDescription,
            List<String> amenities
    ) {}

    private final Map<String, City> cities = Map.ofEntries(
            Map.entry("CPH", new City("København", 750)),
            Map.entry("AAR", new City("Aarhus", 600)),
            Map.entry("AAL", new City("Aalborg", 525)),
            Map.entry("BLL", new City("Billund", 675)),
            Map.entry("OSL", new City("Oslo", 825)),
            Map.entry("BGO", new City("Bergen", 715)),
            Map.entry("TRD", new City("Trondheim", 640)),
            Map.entry("SVG", new City("Stavanger", 675)),
            Map.entry("ARN", new City("Stockholm", 750)),
            Map.entry("GOT", new City("Göteborg", 640)),
            Map.entry("MMX", new City("Malmö", 565)),
            Map.entry("UME", new City("Umeå", 525))
    );

    public List<HotelOffer> search(
            String destination,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults
    ) {
        City city = cities.get(destination);

        if (city == null) {
            throw new IllegalArgumentException(
                    "Ukendt hoteldestination."
            );
        }

        if (checkIn == null || checkOut == null
                || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                    "Udtjekning skal være efter indtjekning."
            );
        }

        if (adults < 1 || adults > 9) {
            throw new IllegalArgumentException(
                    "Vælg mellem 1 og 9 voksne."
            );
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        int rooms = (adults + 1) / 2;

        return List.of(
                createOffer(
                        destination + "-budget",
                        city.name() + " Budget Hotel (test)",
                        destination, city, city.basePrice(),
                        checkIn, checkOut, adults, rooms, nights,
                        "7.4",
                        "Testvej 10, " + city.name()
                                + " (fiktiv adresse)",
                        "Uden for centrum, tæt på offentlig transport.",
                        List.of("Wi-Fi", "Eget badeværelse")
                ),
                createOffer(
                        destination + "-city",
                        city.name() + " City Hotel (test)",
                        destination, city, city.basePrice() + 300,
                        checkIn, checkOut, adults, rooms, nights,
                        "8.6",
                        "Testpladsen 20, " + city.name()
                                + " (fiktiv adresse)",
                        "Centralt med gåafstand til butikker og caféer.",
                        List.of(
                                "Wi-Fi",
                                "Eget badeværelse",
                                "Morgenmad inkluderet"
                        )
                ),
                createOffer(
                        destination + "-premium",
                        city.name() + " Premium Hotel (test)",
                        destination, city, city.basePrice() + 750,
                        checkIn, checkOut, adults, rooms, nights,
                        "9.3",
                        "Testparken 30, " + city.name()
                                + " (fiktiv adresse)",
                        "Roligt område tæt på centrum og grønne områder.",
                        List.of(
                                "Wi-Fi",
                                "Eget badeværelse",
                                "Morgenmad inkluderet",
                                "Fitness",
                                "Spa"
                        )
                )
        );
    }

    private HotelOffer createOffer(
            String id,
            String name,
            String destination,
            City city,
            int nightlyPrice,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int rooms,
            long nights,
            String rating,
            String address,
            String locationDescription,
            List<String> amenities
    ) {
        BigDecimal price = BigDecimal.valueOf(nightlyPrice)
                .setScale(2);

        BigDecimal total = price
                .multiply(BigDecimal.valueOf(rooms))
                .multiply(BigDecimal.valueOf(nights));

        return new HotelOffer(
                id,
                name,
                destination,
                city.name(),
                checkIn.toString(),
                checkOut.toString(),
                adults,
                rooms,
                nights,
                price,
                total,
                "DKK",
                true,
                new BigDecimal(rating),
                address,
                locationDescription,
                amenities
        );
    }
}