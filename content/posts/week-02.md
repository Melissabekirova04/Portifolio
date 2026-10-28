
---
title: "Week 2 – JPA Relationships"
weight: 6
draft: false
summary: "Understanding entity relationships in JPA and connecting users to their trips in Travel Planner."
---

## 1. Topic and Project Connection

During the second week, we focused on relationships in JPA and how different Java entities can be connected through a relational database.

In Travel Planner, relationships are important because a user should be able to create and manage multiple trips, while each trip belongs to a specific user.

JPA allows these relationships to be represented directly in Java using annotations.

## 2. JPA Relationships

JPA supports different types of relationships between entities:

- **One-to-One (`@OneToOne`):** One entity is associated with one other entity.
- **One-to-Many (`@OneToMany`):** One entity can be associated with multiple entities.
- **Many-to-One (`@ManyToOne`):** Multiple entities can reference the same entity.
- **Many-to-Many (`@ManyToMany`):** Multiple entities can be associated with multiple other entities.

Relationships are defined using JPA annotations and mapped to foreign keys or join tables in the database.

For example, a one-to-many relationship can connect one user to several trips.

## 3. How I Use JPA Relationships in Travel Planner

In my Travel Planner backend, I use a relationship between the `User` and `Trip` entities.

The relationship is based on the following structure:

- **One User → Many Trips:** A registered user can create and save multiple trips.
- **Many Trips → One User:** Each trip belongs to one registered user.

This can be represented using `@OneToMany` and `@ManyToOne` annotations in JPA.

The relationship allows the backend to associate saved trips with the correct user.

I also use relationships in the broader data model to connect trips with related information, such as flights, hotels, budgets and activities.

These relationships help organize the database and keep related information connected.

## 4. Reflection

During this week, I learned how JPA relationships make it possible to connect different entities and represent real-world relationships in a relational database.

I also gained a better understanding of the difference between one-to-one, one-to-many and many-to-one relationships.

For Travel Planner, the relationship between users and trips is especially important because each user needs access to their own saved travel plans.

One important consideration is how relationships are mapped and managed by Hibernate. Incorrect mappings can cause problems when saving or retrieving related data.

Understanding JPA relationships helped me see how the database structure can support a larger backend system with several connected entities.
