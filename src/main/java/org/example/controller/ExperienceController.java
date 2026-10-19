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

    // En oplevelse kan tilhøre flere interessekategorier.
    private static final Map<String, List<String>> INTERESTS =
            Map.ofEntries(
                    Map.entry(
                            "CPH-tivoli",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "CPH-nyhavn",
                            List.of("sevaerdigheder", "mad")
                    ),
                    Map.entry(
                            "CPH-christiania",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "AAR-aros",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "AAR-den-gamle-by",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "AAR-moesgaard",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "AAL-utzon",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "AAL-kunsten",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "AAL-zoo",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "BLL-legoland",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "BLL-lego-house",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "BLL-wow-park",
                            List.of("natur", "familie")
                    ),
                    Map.entry(
                            "OSL-opera",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "OSL-vigeland",
                            List.of("sevaerdigheder", "natur")
                    ),
                    Map.entry(
                            "OSL-munch",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "BGO-bryggen",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "BGO-floyen",
                            List.of("natur", "sevaerdigheder")
                    ),
                    Map.entry(
                            "BGO-fisketorget",
                            List.of("mad")
                    ),
                    Map.entry(
                            "TRD-nidaros",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "TRD-bakklandet",
                            List.of("sevaerdigheder", "mad")
                    ),
                    Map.entry(
                            "TRD-rockheim",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "SVG-gamle-stavanger",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "SVG-oliemuseum",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "SVG-kunstmuseum",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "ARN-vasa",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "ARN-skansen",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "ARN-gamla-stan",
                            List.of("sevaerdigheder", "mad")
                    ),
                    Map.entry(
                            "GOT-liseberg",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "GOT-universeum",
                            List.of("sevaerdigheder", "familie")
                    ),
                    Map.entry(
                            "GOT-haga",
                            List.of("sevaerdigheder", "mad")
                    ),
                    Map.entry(
                            "MMX-malmoehus",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "MMX-vastra-hamnen",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "MMX-ribersborg",
                            List.of("natur", "familie")
                    ),
                    Map.entry(
                            "UME-bildmuseet",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "UME-vasterbotten",
                            List.of("sevaerdigheder")
                    ),
                    Map.entry(
                            "UME-umedalen",
                            List.of("sevaerdigheder", "natur")
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

            // Filtrene bruges kun til dette svar.
            // Ingen rejse eller rejsekladde ændres.
            ctx.json(response);
        });

        // Åbn én oplevelse med alle oplysninger.
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
            // Understøtter både kommaseparerede kategorier
            // og gentagne categories-parametre.
            for (String part : value.split(",", -1)) {
                String category = normalizeCategory(part);

                // Et tomt filter svarer til ingen filtrering.
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