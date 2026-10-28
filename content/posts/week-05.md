
---
title: "Week 5 – SP1 Movie Repository"
weight: 3
draft: false
summary: "Working with JPA, external API integration, DTOs, DAO operations, testing and collaboration through the Movie Repository project."
---

## 1. Topic and Project Connection

During the fifth week, we worked on SP1 – Movie Repository, a group assignment that I completed together with a classmate.

The purpose of the assignment was to develop a Java backend that could retrieve movie information from the TMDb API and store the data in a relational database using JPA and Hibernate.

The assignment combined several topics from the previous weeks:

- **JPA and Hibernate:** Storing and retrieving data from a relational database.
- **External APIs:** Fetching movie information from the TMDb API.
- **DTOs:** Converting JSON responses into Java objects.
- **DAOs:** Managing database operations.
- **Service Layer:** Handling application logic and data conversion.
- **Testing:** Testing database and service functionality using JUnit and Testcontainers.
- **Collaboration:** Working together on a shared backend project.

These concepts are also relevant to my individual Travel Planner project, where I work with external APIs, PostgreSQL and a layered backend architecture.

## 2. Movie Repository and Data Integration

The Movie Repository assignment focused on collecting movie information from an external API and storing it in our own database.

The main objective was to retrieve information about Danish movies released within the last five years.

The data included:

- Movie titles and release dates.
- Actors and directors.
- Movie genres.
- Ratings and popularity.

The intended data integration process was:

1. Send HTTP requests to the TMDb API.
2. Receive movie information in JSON format.
3. Convert the JSON responses into DTOs using Jackson.
4. Convert the DTOs into JPA entities in the service layer.
5. Store the entities in the database using DAO methods.
6. Retrieve and manage the stored movie information.

The assignment also included implementing CRUD operations:

- **Create:** Add new movies to the database.
- **Read:** Retrieve stored movies.
- **Update:** Modify existing movie information.
- **Delete:** Remove movies from the database.

Other requirements included searching for movies by title and retrieving information about ratings and popularity.

An important part of the assignment was understanding how the different application layers communicate while keeping their responsibilities separated.

## 3. How SP1 Connects to Travel Planner

Although Movie Repository and Travel Planner have different purposes, they share several technical concepts.

### External API Integration

In Movie Repository, we worked with the TMDb API to retrieve movie information.

In Travel Planner, I use external APIs such as:

- **Open-Meteo:** Retrieves weather information.
- **Duffel:** Retrieves flight information.

Both projects involve sending HTTP requests, processing JSON responses and converting external data into a format that the backend can use.

### Database Persistence

Movie Repository uses JPA and Hibernate to store movie-related information in a relational database.

In Travel Planner, I use Hibernate/JPA and PostgreSQL to store users and their saved trips.

Both projects demonstrate how Java entities can be mapped to database tables and managed through DAO methods.

### Layered Architecture

The projects use similar principles for separating responsibilities.

- **Controllers:** Handle incoming HTTP requests and responses.
- **Services:** Contain application logic and process data.
- **DAOs:** Handle database operations.
- **Entities:** Represent information stored in the database.
- **DTOs:** Transfer data between application components and external systems.

Separating these responsibilities makes the code easier to understand, maintain and test.

### Testing

SP1 included automated testing as an important part of the assignment.

The requirements involved using JUnit and Testcontainers to test database operations and service functionality.

This is also relevant to Travel Planner, where automated tests help verify that backend features work correctly.

### Collaboration

Unlike Travel Planner, which is my individual semester project, SP1 was completed together with a classmate.

Working together gave us the opportunity to discuss technical solutions, share responsibilities and solve problems collaboratively.

It also showed the importance of communication and maintaining a clear project structure when more than one developer works on the same codebase.

## 4. Reflection

During SP1, I gained practical experience with combining external API integration, JPA, DTOs, DAOs and service layers in one backend project.

One of the most important learning experiences was understanding the difference between DTOs and entities.

DTOs are used to transfer data, while entities represent the information stored in the database.

I also developed a better understanding of how the service layer connects external data with database operations.

Working together with a classmate was another important part of the assignment.

It gave me experience in discussing different approaches, dividing responsibilities and solving technical challenges together.

I learned that communication and a clear project structure are important when several developers contribute to the same project.

SP1 was particularly useful because it brought together several topics from the previous weeks and demonstrated how they work together in practice.

I can apply this knowledge to Travel Planner when working with external APIs, database persistence, application architecture and automated testing.

Overall, SP1 helped me strengthen both my technical understanding and my collaboration skills, which are valuable when developing larger backend applications.
