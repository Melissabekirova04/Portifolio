package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import org.example.service.ExperienceService;
import org.example.service.ExperienceService.Experience;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExperienceController {

    private final ExperienceService experienceService =
            new ExperienceService();

    private final ObjectMapper mapper = new ObjectMapper();

    public record InterestCategory(String id, String name) {}

    private static final List<InterestCategory> CATEGORIES = List.of(
            new InterestCategory("mad", "Mad"),
            new InterestCategory("sevaerdigheder", "Seværdigheder"),
            new InterestCategory("shopping", "Shopping"),
            new InterestCategory("natur", "Natur"),
            new InterestCategory("natteliv", "Natteliv"),
            new InterestCategory("familie", "Familie")
    );

    private static final Map<String, List<String>> INTERESTS =
            Map.ofEntries(
                    Map.entry(
                            "CPH-tivoli",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "CPH-nyhavn",
                            List.of("serverdigheder", "mad")
                    ),
                    Map.entry(
                            "CPH-christiania",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "AAR-aros",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "AAR-den-gamle-by",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "AAR-moesgaard",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "AAL-utzon",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "AAL-kunsten",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "AAL-zoo",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "BLL-legoland",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "BLL-lego-house",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "BLL-wow-park",
                            List.of("natur", "familie")
                    ),
                    Map.entry(
                            "OSL-opera",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "OSL-vigeland",
                            List.of("serverdigheder", "natur")
                    ),
                    Map.entry(
                            "OSL-munch",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "BGO-bryggen",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "BGO-floyen",
                            List.of("natur", "serverdigheder")
                    ),
                    Map.entry(
                            "BGO-fisketorget",
                            List.of("mad")
                    ),
                    Map.entry(
                            "TRD-nidaros",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "TRD-bakklandet",
                            List.of("serverdigheder", "mad")
                    ),
                    Map.entry(
                            "TRD-rockheim",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "SVG-gamle-stavanger",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "SVG-oliemuseum",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "SVG-kunstmuseum",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "ARN-vasa",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "ARN-skansen",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "ARN-gamla-stan",
                            List.of("serverdigheder", "mad")
                    ),
                    Map.entry(
                            "GOT-liseberg",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "GOT-universeum",
                            List.of("serverdigheder", "familie")
                    ),
                    Map.entry(
                            "GOT-haga",
                            List.of("serverdigheder", "mad")
                    ),
                    Map.entry(
                            "MMX-malmoehus",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "MMX-vastra-hamnen",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "MMX-ribersborg",
                            List.of("natur", "familie")
                    ),
                    Map.entry(
                            "UME-bildmuseet",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "UME-vasterbotten",
                            List.of("serverdigheder")
                    ),
                    Map.entry(
                            "UME-umedalen",
                            List.of("serverdigheder", "natur")
                    )
            );

    public void registerRoutes(Javalin app) {

        // Kategorier, som frontend kan vise som valgmuligheder.
        app.get("/api/experience-categories", ctx -> {
            ctx.json(CATEGORIES);
        });

        // US10 + US11: Vis og filtrer destinationens oplevelser.
        app.get("/api/destinations/{code}/experiences", ctx -> {
            String code = ctx.pathParam("code")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!experienceService.supportsDestination(code)) {
                throw new NotFoundResponse(
                        "Destinationen findes ikke."
                );
            }

            Set<String> selectedCategories = parseCategories(
                    ctx.queryParams("categories")
            );

            List<Experience> all =
                    experienceService.findByDestination(code);

            List<Experience> filtered = all.stream()
                    .filter(experience ->
                            selectedCategories.isEmpty()
                                    || interestsFor(experience).stream()
                                    .anyMatch(selectedCategories::contains))
                    .toList();

            ArrayNode items = mapper.createArrayNode();

            for (Experience experience : filtered) {
                items.add(toListItem(experience));
            }

            ObjectNode response = mapper.createObjectNode();

            response.put("destination", code);
            response.put("count", filtered.size());
            response.put("totalBeforeFiltering", all.size());
            response.put("source", "Lokalt udvalg af seværdigheder");
            response.put("filterMode", "any");

            response.set(
                    "selectedCategories",
                    mapper.valueToTree(
                            new ArrayList<>(selectedCategories)
                    )
            );

            response.set(
                    "availableCategories",
                    mapper.valueToTree(CATEGORIES)
            );

            response.set("experiences", items);

            if (filtered.isEmpty()) {
                response.put(
                        "message",
                        "Ingen oplevelser matcher de valgte kategorier "
                                + "på denne destination."
                );
            }

            ctx.json(response);
        });

        app.get("/api/experiences/{id}", ctx -> {
            Experience experience = experienceService
                    .findById(ctx.pathParam("id"))
                    .orElseThrow(() -> new NotFoundResponse(
                            "Oplevelsen findes ikke."
                    ));

            ObjectNode response = mapper.valueToTree(experience);

            response.set(
                    "interestCategories",
                    mapper.valueToTree(interestsFor(experience))
            );

            ctx.json(response);
        });
    }

    private ObjectNode toListItem(Experience experience) {
        ObjectNode item = mapper.createObjectNode();

        item.put("id", experience.id());
        item.put("destination", experience.destination());
        item.put("name", experience.name());
        item.put("category", experience.category());
        item.put("description", experience.description());

        item.set(
                "interestCategories",
                mapper.valueToTree(interestsFor(experience))
        );

        item.set(
                "rating",
                mapper.valueToTree(experience.rating())
        );

        item.set(
                "priceAmount",
                mapper.valueToTree(experience.priceAmount())
        );

        item.set(
                "currency",
                mapper.valueToTree(experience.currency())
        );

        item.put("priceNotice", experience.priceNotice());

        item.put(
                "detailsUrl",
                "/api/experiences/" + experience.id()
        );

        return item;
    }

    private List<String> interestsFor(Experience experience) {
        return INTERESTS.getOrDefault(
                experience.id(),
                List.of()
        );
    }

    private Set<String> parseCategories(List<String> values) {
        Set<String> selected = new LinkedHashSet<>();

        for (String value : values) {
            for (String part : value.split(",", -1)) {
                String category = normalizeCategory(part);

                if (category.isBlank()) {
                    continue;
                }

                boolean supported = CATEGORIES.stream()
                        .anyMatch(item -> item.id().equals(category));

                if (!supported) {
                    throw new BadRequestResponse(
                            "Ukendt kategori: " + part.trim()
                                    + ". Vælg mad, sevaerdigheder, shopping, "
                                    + "natur, natteliv eller familie."
                    );
                }

                selected.add(category);
            }
        }

        return selected;
    }

    private String normalizeCategory(String value) {
        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("æ", "ae")
                .replace("ø", "o")
                .replace("å", "a");

        return Normalizer.normalize(
                normalized,
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");
    }
}