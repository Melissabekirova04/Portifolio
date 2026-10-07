---
title: "Week 2 – JPA II"
weight: 6
draft: false
summary: "Connecting Hibernate to PostgreSQL and separating database access."
---

## Topic and Project Connection

The second JPA topic connects to how my backend manages database access, queries and transactions.

In Travel Planner, HibernateConfig creates an EntityManagerFactory using the database connection settings. The application connects to the PostgreSQL database named travelplanner.

## Configuration and DAO

The database URL, username and password are read from environment variables. This allows the connection settings to be configured outside the Java source code.

UserDAO handles database operations for users. It contains methods for creating a user and finding a user by email.

Creating a user requires a transaction. The transaction is committed when the operation succeeds and rolled back if an error occurs. The EntityManager is closed after the operation.

## Reflection

Separating database access into a DAO makes the responsibilities clearer. The controller handles HTTP requests, while the DAO handles persistence.

A unique email constraint also protects the database from storing multiple users with the same email address.

Relationships between users and trips are a next step. These relationships are not implemented yet.