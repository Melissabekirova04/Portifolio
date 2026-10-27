package org.example.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Activity;

import java.math.BigDecimal;

public record ActivityDTO(
        String id,
        String name,
        String date,
        String time,
        BigDecimal priceAmount,
        String currency,
        String category,
        String notes,
        String type,
        String experienceId,
        String destination,
        String addedAt,
        String updatedAt,
        String timeBasis,
        String description,
        String details,
        Double rating,
        String priceNotice,
        String sourceUrl
) {

    public static ActivityDTO fromEntity(Activity activity) {
        JsonNode source = readSource(activity.getSourceJson());

        Double rating = null;

        if (source.path("rating").isNumber()) {
            rating = source.path("rating").doubleValue();
        }

        String priceNotice = source.path("priceNotice")
                .asText(null);

        /*
         * En egen aktivitet eller en redigeret aktivitet
         * bruger den pris, som brugeren selv har angivet.
         */
        if ("custom".equals(activity.getType())
                || activity.getUpdatedAt() != null) {
            priceNotice =
                    "Brugerens egen planlægningspris. "
                            + "null betyder ukendt pris; 0 betyder gratis.";
        }

        return new ActivityDTO(
                activity.getId(),
                activity.getName(),
                activity.getDate().toString(),
                activity.getTime().toString(),
                activity.getPriceAmount(),
                activity.getCurrency(),
                activity.getCategory(),
                activity.getNotes(),
                activity.getType(),
                activity.getExperienceId(),
                activity.getDestination(),
                activity.getAddedAt().toString(),
                activity.getUpdatedAt() == null
                        ? null
                        : activity.getUpdatedAt().toString(),
                "Lokal tid på destinationen",
                source.path("description").asText(null),
                source.path("details").asText(null),
                rating,
                priceNotice,
                source.path("sourceUrl").asText(null)
        );
    }

    private static JsonNode readSource(String sourceJson) {
        ObjectMapper mapper = new ObjectMapper();

        if (sourceJson == null || sourceJson.isBlank()) {
            return mapper.createObjectNode();
        }

        try {
            JsonNode source = mapper.readTree(sourceJson);

            if (source == null || !source.isObject()) {
                throw new IllegalStateException(
                        "Aktivitetens kildeoplysninger "
                                + "har et ugyldigt format."
                );
            }

            return source;

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Kunne ikke læse aktivitetens kildeoplysninger.",
                    e
            );
        }
    }
}