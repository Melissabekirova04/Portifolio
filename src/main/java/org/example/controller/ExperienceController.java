package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import org.example.service.ExperienceService;
import org.example.service.ExperienceService.Experience;

import java.util.List;
import java.util.Locale;

public class ExperienceController {

    private final ExperienceService experienceService =
            new ExperienceService();

    private final ObjectMapper mapper = new ObjectMapper();

    public void registerRoutes(Javalin app) {

        // Vis forslag til den valgte destination.
        app.get("/api/destinations/{code}/experiences", ctx -> {
            String code = ctx.pathParam("code")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!experienceService.supportsDestination(code)) {
                throw new NotFoundResponse(
                        "Destinationen findes ikke."
                );
            }

            List<Experience> experiences =
                    experienceService.findByDestination(code);

            ArrayNode items = mapper.createArrayNode();

            for (Experience experience : experiences) {
                ObjectNode item = mapper.createObjectNode();

                item.put("id", experience.id());
                item.put("destination", experience.destination());
                item.put("name", experience.name());
                item.put("category", experience.category());
                item.put("description", experience.description());

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

                items.add(item);
            }

            ObjectNode response = mapper.createObjectNode();

            response.put("destination", code);
            response.put("count", experiences.size());
            response.put("source", "Lokalt udvalg af seværdigheder");
            response.set("experiences", items);

            ctx.json(response);
        });

        // Åbn ét forslag og se flere oplysninger.
        app.get("/api/experiences/{id}", ctx -> {
            Experience experience = experienceService
                    .findById(ctx.pathParam("id"))
                    .orElseThrow(() -> new NotFoundResponse(
                            "Oplevelsen findes ikke."
                    ));

            ctx.json(experience);
        });
    }
}