---
title: "Week 1 – JPA I"
weight: 7
draft: false
summary: "Introducing database persistence and the foundation of Travel Planner."
---

## Topic and Project Connection

The first topic was JPA and how Java objects can be mapped to database tables.

My semester project is Travel Planner, a Java backend that will help users plan and manage personal trips. Database persistence is important because user accounts and saved trips must remain available after the application stops.

## Database Persistence

JPA defines how Java objects can be stored and retrieved from a relational database. Hibernate is the implementation used in my project, while PostgreSQL stores the data.

The project includes a User entity with an ID, name, email and password hash. Annotations such as @Entity and @Id describe how Hibernate should map the class to the database.

## Reflection

Compared with writing SQL manually through JDBC, JPA introduces a different way of working with database data. I need to understand both the Java objects and the database structure behind them.

The User entity provides a starting point for persistence. Additional entities will be needed when I implement saved trips.