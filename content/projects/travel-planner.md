---
title: "Travel Planner"
category: "3rd Semester · Backend Project"
summary: "A third-semester Java backend project for travel planning, integrating flights, hotels, weather data, user authentication and PostgreSQL."
weight: 1
---

Travel Planner is my individual third-semester project, developed as part of my Computer Science AP degree.

The project focuses on backend development using Java and Javalin. It provides a REST API for creating and managing personal trips, searching for flights and hotels, retrieving weather information and storing travel data in PostgreSQL.

The backend follows a layered architecture with controllers, services, DAOs and entities. The project focuses on authentication, database management, external API integration, automated testing and deployment.

## Implemented Features

- User registration, login and logout with session-based authentication.
- Secure password hashing and protected endpoints.
- Creation, storage and retrieval of personal trips.
- Destination search and selection.
- Flight searches using the Duffel API.
- Hotel searches and hotel selection.
- Weather information using the Open-Meteo API.
- Travel budget management.
- PostgreSQL database integration using Hibernate/JPA.
- Automated testing with JUnit.
- Backend deployment using Docker and DigitalOcean.

## Technologies

**Java, Javalin, Hibernate/JPA, PostgreSQL, Maven, Jackson, Password4j, JUnit, Docker and DigitalOcean.**



## Development Journal

These entries document the development of Travel Planner, the technical topics connected to the project and my reflections.

- [Week 1 – JPA I]({{< relref "/posts/week-01.md" >}})
- [Week 2 – JPA II]({{< relref "/posts/week-02.md" >}})
- [Week 3 – Data Integration & External API]({{< relref "/posts/week-03.md" >}})
- [Week 4 – Concurrency]({{< relref "/posts/week-04.md" >}})
- [Week 5 – SP1 & Project Structure]({{< relref "/posts/week-05.md" >}})
- [Week 6 – REST I]({{< relref "/posts/week-06.md" >}})
- [Week 7 – REST & Test]({{< relref "/posts/week-07.md" >}})