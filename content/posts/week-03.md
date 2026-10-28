
---
title: "Week 3 – Data Integration"
weight: 5
draft: false
summary: "Learning data integration by connecting the Travel Planner backend to the Open-Meteo weather API."
---

## 1. Topic and Project Connection

During the third week, we focused on data integration and how applications can communicate with external systems to retrieve and process information.

In my Travel Planner backend, data integration is important because travel planning requires information that is not stored in my own database.

I chose to integrate weather information to help users get an overview of weather conditions at their travel destinations.

For this functionality, I use the Open-Meteo API to retrieve weather data for supported airport cities in Scandinavia.

## 2. Data Integration and External APIs

Data integration allows different systems to exchange information.

An API (Application Programming Interface) provides a way for applications to communicate with each other.

In Travel Planner, I use a REST API to request weather information from Open-Meteo.

The integration works as follows:

1. My backend sends an HTTP request to Open-Meteo using the destination's latitude and longitude.
2. Open-Meteo returns weather information in JSON format.
3. Jackson is used to read and process the JSON response.
4. The backend extracts relevant information such as temperature, wind speed and weather codes.
5. The weather code is converted into a readable description.
6. The processed weather information is returned through my own backend API.

This allows Travel Planner to provide weather information without maintaining its own weather database.

## 3. How I Use Data Integration in Travel Planner

I separated the weather integration into three main components:

**WeatherApiClient**
- Communicates directly with the Open-Meteo API.
- Sends HTTP requests using geographical coordinates.
- Reads the JSON response using Jackson.

**WeatherService**
- Manages the supported airport cities and their coordinates.
- Processes the weather information received from the external API.
- Converts weather codes into readable descriptions.

**WeatherController**
- Defines the weather endpoints using Javalin.
- Receives incoming requests.
- Uses WeatherService to retrieve the weather information.
- Returns the result to the client as JSON.

This separation follows the layered architecture used in my backend project.

It also makes the code easier to maintain because the external API communication is separated from the application logic and HTTP endpoints.

## 4. Reflection

During this week, I learned how a Java backend can communicate with an external API and use the returned data as part of its own functionality.

One of the most important things I learned was how JSON responses can be processed using Jackson and transformed into information that my own backend can return.

I also gained a better understanding of why separating controllers, services and API clients is useful.

An important challenge with external APIs is that my application depends on another service being available. Network errors, unavailable services or unexpected responses can affect the functionality.

This week helped me understand how data integration works in practice and provided a foundation for integrating other external services, such as flight and hotel APIs, later in the project.
