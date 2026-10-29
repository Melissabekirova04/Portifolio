package org.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import org.example.dao.TripDAO;
import org.example.entity.Trip;
import org.example.security.SecurityMiddleware;
import org.example.service.WeatherService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class WeatherController {

    private final WeatherService weatherService = new WeatherService();
    private final TripDAO tripDAO = new TripDAO();
    private final ObjectMapper mapper = new ObjectMapper();

    public void registerRoutes(Javalin app) {

        app.get("/api/weather/airports", ctx ->
                ctx.json(weatherService.getAllAirportWeather())
        );

        app.get("/api/weather/airports/{code}", ctx ->
                ctx.json(weatherService.getWeatherByAirportCode(
                        ctx.pathParam("code")
                ))
        );


        app.get("/api/weather/destinations/{code}", ctx ->
                ctx.json(weatherService.getOverview(
                        ctx.pathParam("code")
                ))
        );


        app.get("/api/weather/destinations/{code}/climate", ctx ->
                ctx.json(weatherService.getClimate(
                        ctx.pathParam("code")
                ))
        );


        app.get(
                "/api/weather/destinations/{code}/recommendations",
                ctx -> ctx.json(
                        weatherService.getRecommendations(
                                ctx.pathParam("code"),
                                optionalDate(ctx.queryParam("date"))
                        )
                )
        );


        app.get("/api/trip-draft/weather", ctx -> {
            SecurityMiddleware.requireUserId(ctx);

            Object destination =
                    ctx.sessionAttribute("tripDestination");

            Object search = ctx.sessionAttribute("flightSearch");

            if (destination == null || search == null) {
                throw new BadRequestResponse(
                        "Vælg destination og rejsedatoer først."
                );
            }

            JsonNode destinationData =
                    mapper.valueToTree(destination);

            JsonNode searchData = mapper.valueToTree(search);

            String code =
                    destinationData.path("airportCode").asText();

            ctx.json(planningWeather(
                    code,
                    searchData,
                    ctx.queryParam("date")
            ));
        });


        app.get("/api/trips/{id}/weather", ctx -> {
            Long userId = SecurityMiddleware.requireUserId(ctx);
            Long tripId = parseTripId(ctx.pathParam("id"));

            Trip trip = tripDAO.findByIdAndUserId(tripId, userId)
                    .orElseThrow(() -> new NotFoundResponse(
                            "Rejsen findes ikke."
                    ));

            JsonNode destination =
                    mapper.readTree(trip.getDestinationJson());

            JsonNode search = trip.getSearchJson() == null
                    ? mapper.nullNode()
                    : mapper.readTree(trip.getSearchJson());

            ObjectNode response = planningWeather(
                    destination.path("airportCode").asText(),
                    search,
                    ctx.queryParam("date")
            );

            response.put("tripId", tripId);

            ctx.json(response);
        });
    }

    private ObjectNode planningWeather(
            String code,
            JsonNode search,
            String requestedDate
    ) {
        LocalDate start = requiredDate(
                search.path("departureDate").asText()
        );

        LocalDate end = requiredDate(
                search.path("returnDate").asText()
        );

        if (!end.isAfter(start)) {
            throw new BadRequestResponse(
                    "Rejsen mangler gyldige start- og slutdatoer."
            );
        }

        if (!code.equalsIgnoreCase(
                search.path("destination").asText()
        )) {
            throw new BadRequestResponse(
                    "Destinationen matcher ikke rejseoplysningerne."
            );
        }

        LocalDate selectedDate = optionalDate(requestedDate);

        if (selectedDate == null) {
            selectedDate = start;
        }

        if (selectedDate.isBefore(start)
                || selectedDate.isAfter(end)) {
            throw new BadRequestResponse(
                    "Vælg en dato inden for rejsens datoer."
            );
        }

        ObjectNode response = weatherService.getOverview(code);

        response.put("tripStartDate", start.toString());
        response.put("tripEndDate", end.toString());
        response.put("selectedDate", selectedDate.toString());
        response.put("selectedMonth", selectedDate.getMonthValue());

        response.set(
                "recommendations",
                weatherService.getRecommendations(code, selectedDate)
        );

        response.put(
                "climateNotice",
                "Hent climateUrl for alle 12 månedsgennemsnit. "
                        + "Fremhæv selectedMonth i visningen. "
                        + "Månedsgennemsnit er ikke en prognose."
        );

        return response;
    }

    private LocalDate optionalDate(String value) {
        return value == null ? null : requiredDate(value);
    }

    private LocalDate requiredDate(String value) {
        if (value == null
                || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BadRequestResponse(
                    "Angiv dato som YYYY-MM-DD."
            );
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse(
                    "Datoen er ugyldig."
            );
        }
    }

    private Long parseTripId(String value) {
        try {
            long id = Long.parseLong(value);

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;
        } catch (NumberFormatException e) {
            throw new BadRequestResponse(
                    "Rejsens ID skal være et positivt heltal."
            );
        }
    }
}