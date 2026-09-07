---
title: "Week 3 - Data Integration & External API"
date: 2026-09-04
draft: false
---

## What did I work on this week?

This week I focused on data integration in my Travel Planner project.

The goal was to integrate an external API into the Java backend and use external data as part of the application.

I chose to integrate weather data because weather information is useful when planning a trip.

The Travel Planner can now fetch current weather information for supported airport cities in Scandinavia.

## External API

For the weather integration, I used the Open-Meteo API.

The API allows my backend to request current weather data using latitude and longitude.

The application currently retrieves:

- Temperature
- Wind speed
- Weather code

The weather code is converted into a more understandable description inside the application.

## Application Structure

I separated the weather functionality into different layers so each class has a clear responsibility.

### WeatherApiClient

`WeatherApiClient` is responsible for communicating with the external Open-Meteo API.

It:

- Creates the API request
- Sends the HTTP request
- Receives the JSON response
- Uses Jackson to read the response

### WeatherService

`WeatherService` contains the logic for the weather functionality.

It is responsible for:

- Managing the supported Scandinavian airport cities
- Storing their coordinates
- Calling `WeatherApiClient`
- Processing the weather data
- Converting weather codes into readable descriptions

### WeatherController

`WeatherController` exposes the weather functionality through REST endpoints using Javalin.

One endpoint returns weather information for all supported airports:

```text
GET /api/weather/airports/CPH