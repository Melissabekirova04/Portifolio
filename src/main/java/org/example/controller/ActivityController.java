package org.example.controller;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.example.dto.ActivityDTO;
import org.example.security.SecurityMiddleware;
import org.example.service.ActivityService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ActivityController {

    private final ActivityService activityService =
            new ActivityService();

    public record ActivityChoice(
            String experienceId,
            String date,
            String time
    ) {}

    public record ActivityInput(
            String name,
            String date,
            String time,
            BigDecimal priceAmount,
            String category,
            String notes
    ) {}

    public void registerRoutes(Javalin app) {
        app.post(
                "/api/trips/{id}/activities",
                this::createRecommended
        );

        app.post(
                "/api/trips/{id}/activities/custom",
                this::createCustom
        );

        app.get(
                "/api/trips/{id}/activities",
                this::findAll
        );

        app.put(
                "/api/trips/{id}/activities/{activityId}",
                this::update
        );

        app.delete(
                "/api/trips/{id}/activities/{activityId}",
                this::delete
        );
    }

    private void createRecommended(Context ctx) {
        Long userId = SecurityMiddleware.requireUserId(ctx);
        Long tripId = parseTripId(ctx.pathParam("id"));

        ActivityChoice input =
                ctx.bodyAsClass(ActivityChoice.class);

        if (input == null) {
            throw new BadRequestResponse("Request body mangler.");
        }

        try {
            ActivityDTO activity =
                    activityService.createRecommended(
                            tripId,
                            userId,
                            input.experienceId(),
                            input.date(),
                            input.time()
                    );

            ctx.status(201).json(Map.of(
                    "tripId", tripId,
                    "message", "Oplevelsen er tilføjet til rejsen.",
                    "activity", activity
            ));

        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of(
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    private void createCustom(Context ctx) {
        Long userId = SecurityMiddleware.requireUserId(ctx);
        Long tripId = parseTripId(ctx.pathParam("id"));

        ActivityInput input =
                ctx.bodyAsClass(ActivityInput.class);

        if (input == null) {
            throw new BadRequestResponse("Request body mangler.");
        }

        try {
            ActivityDTO activity = activityService.createCustom(
                    tripId,
                    userId,
                    input.name(),
                    input.date(),
                    input.time(),
                    input.priceAmount(),
                    input.category(),
                    input.notes()
            );

            ctx.status(201).json(Map.of(
                    "tripId", tripId,
                    "message", "Din aktivitet er oprettet.",
                    "activity", activity
            ));

        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of(
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    private void findAll(Context ctx) {
        Long userId = SecurityMiddleware.requireUserId(ctx);
        Long tripId = parseTripId(ctx.pathParam("id"));

        try {
            List<ActivityDTO> activities =
                    activityService.findAll(tripId, userId);

            ctx.json(Map.of(
                    "tripId", tripId,
                    "count", activities.size(),
                    "activities", activities
            ));

        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of(
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    private void update(Context ctx) {
        Long userId = SecurityMiddleware.requireUserId(ctx);
        Long tripId = parseTripId(ctx.pathParam("id"));
        String activityId = ctx.pathParam("activityId");

        ActivityInput input =
                ctx.bodyAsClass(ActivityInput.class);

        if (input == null) {
            throw new BadRequestResponse("Request body mangler.");
        }

        try {
            ActivityDTO activity = activityService.update(
                    activityId,
                    tripId,
                    userId,
                    input.name(),
                    input.date(),
                    input.time(),
                    input.priceAmount(),
                    input.category(),
                    input.notes()
            );

            ctx.json(Map.of(
                    "tripId", tripId,
                    "message", "Aktiviteten er opdateret.",
                    "activity", activity
            ));

        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of(
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    private void delete(Context ctx) {
        Long userId = SecurityMiddleware.requireUserId(ctx);
        Long tripId = parseTripId(ctx.pathParam("id"));
        String activityId = ctx.pathParam("activityId");

        try {
            activityService.delete(activityId, tripId, userId);

            ctx.json(Map.of(
                    "tripId", tripId,
                    "deletedActivityId", activityId,
                    "message", "Aktiviteten er slettet."
            ));

        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of(
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of(
                    "message", e.getMessage()
            ));
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