---
title: "Week 3 – Data Integration & External API"
weight: 5
draft: false
summary: "Integrating weather information from Open-Meteo into Travel Planner."
---

## Topic and Project Connection

Data integration connects my backend with information from an external service.

For Travel Planner, I chose weather information because it can help users prepare for a trip. The weather functionality retrieves current weather for supported airport cities in Scandinavia.

## External API

The integration uses the Open-Meteo API. Requests use latitude and longitude to retrieve temperature, wind speed and a weather code.

The weather code is converted into a readable description inside the application.

## Application Structure

WeatherApiClient communicates with the external API and reads the JSON response using Jackson.

WeatherService manages the supported airport cities and their coordinates. It also processes the weather information and converts weather codes into descriptions.

WeatherController exposes the functionality through Javalin endpoints.

## Reflection

This feature helped me understand how external data becomes part of a backend application.

Separating the client, service and controller makes it easier to see where HTTP communication, application logic and endpoint handling belong.

The integration also introduces a dependency on another service. Improving how the backend handles unavailable services and unexpected responses is a next step.