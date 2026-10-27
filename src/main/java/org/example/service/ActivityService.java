package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.ActivityDAO;
import org.example.dao.TripDAO;
import org.example.dto.ActivityDTO;
import org.example.entity.Activity;
import org.example.entity.Trip;
import org.example.service.ExperienceService.Experience;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ActivityService {

    private final ActivityDAO activityDAO;
    private final TripDAO tripDAO;
    private final ExperienceService experienceService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ActivityService() {
        this(
                new ActivityDAO(),
                new TripDAO(),
                new ExperienceService()
        );
    }

    // Denne constructor kan senere bruges i unit tests.
    public ActivityService(
            ActivityDAO activityDAO,
            TripDAO tripDAO,
            ExperienceService experienceService
    ) {
        this.activityDAO = activityDAO;
        this.tripDAO = tripDAO;
        this.experienceService = experienceService;
    }

    public ActivityDTO createRecommended(
            Long tripId,
            Long userId,
            String experienceId,
            String date,
            String time
    ) {
        Trip trip = requireTrip(tripId, userId);

        if (experienceId == null || experienceId.isBlank()) {
            throw new IllegalArgumentException(
                    "experienceId mangler."
            );
        }

        Experience experience = experienceService
                .findById(experienceId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Oplevelsen findes ikke."
                ));

        String destination = readJson(
                trip.getDestinationJson()
        ).path("airportCode").asText();

        if (!experience.destination().equals(destination)) {
            throw new IllegalArgumentException(
                    "Oplevelsen hører ikke til rejsens destination."
            );
        }

        LocalDate validDate = validateDate(date, trip);
        LocalTime validTime = validateTime(time);
        BigDecimal validPrice = validatePrice(
                experience.priceAmount()
        );

        Activity activity = new Activity(
                experience.name(),
                validDate,
                validTime,
                validPrice,
                experience.currency(),
                experience.category(),
                "",
                "recommended",
                experience.id(),
                destination,
                toJson(experience)
        );

        Activity saved = activityDAO.create(
                tripId, userId, activity
        );

        return ActivityDTO.fromEntity(saved);
    }

    public ActivityDTO createCustom(
            Long tripId,
            Long userId,
            String name,
            String date,
            String time,
            BigDecimal priceAmount,
            String category,
            String notes
    ) {
        Trip trip = requireTrip(tripId, userId);

        String validName = validateName(name);
        LocalDate validDate = validateDate(date, trip);
        LocalTime validTime = validateTime(time);
        BigDecimal validPrice = validatePrice(priceAmount);

        String validCategory = validateText(
                category, "Kategori", 100
        );

        String validNotes = validateText(
                notes, "Noter", 5000
        );

        String destination = readJson(
                trip.getDestinationJson()
        ).path("airportCode").asText();

        Activity activity = new Activity(
                validName,
                validDate,
                validTime,
                validPrice,
                validPrice == null ? null : "DKK",
                validCategory,
                validNotes,
                "custom",
                null,
                destination,
                null
        );

        Activity saved = activityDAO.create(
                tripId, userId, activity
        );

        return ActivityDTO.fromEntity(saved);
    }

    public List<ActivityDTO> findAll(
            Long tripId,
            Long userId
    ) {
        requireTrip(tripId, userId);

        List<Activity> activities = activityDAO.findAll(
                tripId, userId
        );

        List<ActivityDTO> result = new ArrayList<>();

        for (Activity activity : activities) {
            result.add(ActivityDTO.fromEntity(activity));
        }

        return result;
    }

    public ActivityDTO update(
            String activityId,
            Long tripId,
            Long userId,
            String name,
            String date,
            String time,
            BigDecimal priceAmount,
            String category,
            String notes
    ) {
        Trip trip = requireTrip(tripId, userId);

        requireActivity(activityId, tripId, userId);

        String validName = validateName(name);
        LocalDate validDate = validateDate(date, trip);
        LocalTime validTime = validateTime(time);
        BigDecimal validPrice = validatePrice(priceAmount);

        String validCategory = validateText(
                category, "Kategori", 100
        );

        String validNotes = validateText(
                notes, "Noter", 5000
        );

        Activity updated = activityDAO.update(
                activityId,
                tripId,
                userId,
                validName,
                validDate,
                validTime,
                validPrice,
                validPrice == null ? null : "DKK",
                validCategory,
                validNotes
        );

        return ActivityDTO.fromEntity(updated);
    }

    public void delete(
            String activityId,
            Long tripId,
            Long userId
    ) {
        requireTrip(tripId, userId);
        requireActivity(activityId, tripId, userId);

        activityDAO.delete(activityId, tripId, userId);
    }

    private Trip requireTrip(Long tripId, Long userId) {
        return tripDAO.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Rejsen findes ikke."
                ));
    }

    private void requireActivity(
            String activityId,
            Long tripId,
            Long userId
    ) {
        try {
            activityDAO.findById(
                    activityId, tripId, userId
            );
        } catch (IllegalArgumentException e) {
            throw new NoSuchElementException(
                    "Aktiviteten findes ikke på denne rejse."
            );
        }
    }

    private String validateName(String name) {
        String text = validateText(name, "Navn", 200);

        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "Navn skal udfyldes."
            );
        }

        return text;
    }

    private String validateText(
            String value,
            String label,
            int maxLength
    ) {
        String text = value == null ? "" : value.trim();

        if (text.length() > maxLength) {
            throw new IllegalArgumentException(
                    label + " må højst indeholde "
                            + maxLength + " tegn."
            );
        }

        return text;
    }

    private LocalDate validateDate(String value, Trip trip) {
        if (value == null
                || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException(
                    "Angiv dato som YYYY-MM-DD."
            );
        }

        LocalDate date;

        try {
            date = LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Aktivitetens dato er ugyldig."
            );
        }

        JsonNode search = readJson(trip.getSearchJson());

        LocalDate startDate;
        LocalDate endDate;

        try {
            startDate = LocalDate.parse(
                    search.path("departureDate").asText()
            );

            endDate = LocalDate.parse(
                    search.path("returnDate").asText()
            );
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Rejsen mangler gyldige datoer."
            );
        }

        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "Rejsens slutdato skal ligge efter startdatoen."
            );
        }

        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Aktivitetens dato skal ligge mellem "
                            + startDate + " og " + endDate + "."
            );
        }

        return date;
    }

    private LocalTime validateTime(String value) {
        if (value == null
                || !value.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException(
                    "Angiv tidspunkt som HH:mm."
            );
        }

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Aktivitetens tidspunkt er ugyldigt."
            );
        }
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (price == null) {
            return null;
        }

        if (price.signum() < 0) {
            throw new IllegalArgumentException(
                    "Prisen må ikke være negativ."
            );
        }

        if (price.compareTo(
                new BigDecimal("1000000000.00")
        ) > 0) {
            throw new IllegalArgumentException(
                    "Prisen er for høj."
            );
        }

        try {
            return price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Prisen må højst have to decimaler."
            );
        }
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Rejsens oplysninger mangler."
            );
        }

        try {
            JsonNode result = mapper.readTree(value);

            if (result == null || !result.isObject()) {
                throw new IllegalStateException(
                        "Rejsens oplysninger har et ugyldigt format."
                );
            }

            return result;

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke læse rejsens oplysninger.", e
            );
        }
    }

    private String toJson(Experience experience) {
        try {
            return mapper.writeValueAsString(experience);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke gemme oplevelsens kildeoplysninger.",
                    e
            );
        }
    }
}