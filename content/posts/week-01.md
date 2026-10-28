---

---
title: "Week 1 – JPA Basics"
weight: 7
draft: false
summary: "Introduction to JPA, Hibernate and database persistence in Travel Planner."
---

## 1. Topic and Project Connection

During the first week, we were introduced to JPA (Java Persistence API) and how Java objects can be mapped to relational database tables.

My semester project is Travel Planner, a Java backend for planning and managing personal trips.

Database persistence is important for my project because user accounts and saved trips must remain available even after the application has stopped.

## 2. Database Persistence

JPA is a Java specification that defines how Java objects can be stored and retrieved from relational databases.

In my project, I use:

- **JPA:** Defines how Java entities are mapped to database tables.
- **Hibernate:** Implements JPA and handles database operations.
- **PostgreSQL:** Stores the application's data.

By using JPA, I can work with Java objects instead of manually writing SQL queries for every database operation.

## 3. How I Use JPA in Travel Planner

In my Travel Planner backend, I use JPA with Hibernate to connect Java entities to PostgreSQL.

For example, I use a `User` entity to represent registered users.

The entity contains information such as:

- ID
- Name
- Email
- Password hash

I use JPA annotations such as `@Entity` and `@Id` to define how the Java class is mapped to a database table.

Hibernate handles the mapping between the Java objects and PostgreSQL.

This provides the foundation for storing user accounts and later connecting users to their saved trips.

## 4. Reflection

During this week, I gained a better understanding of how JPA and Hibernate connect Java objects to relational database tables.

Compared with JDBC, JPA reduces the amount of SQL and repetitive database code I need to write.

However, I also learned that using JPA requires an understanding of entity mapping and how Hibernate manages database operations.

For Travel Planner, JPA is an important part of the backend because the system needs to store user information and eventually manage relationships between users and trips.

This week gave me a foundation for working with database persistence, which I can build on throughout the semester.
