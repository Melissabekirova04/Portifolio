---
title: "Week 6 – REST I"
weight: 2
draft: false
summary: "Implementing registration, login and protected endpoints with Javalin."
---

## Topic and Project Connection

The REST topic connects directly to how clients communicate with my Java backend through HTTP.

Travel Planner uses Javalin to expose endpoints and JSON to receive and return data.

## Registration and Login

The authentication endpoints include:

- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout
- GET /api/auth/me

Registration receives a name, email and password through RegisterRequest. The backend validates the input and checks whether the email is already registered.

The password is hashed with bcrypt using Password4j before the user is stored in PostgreSQL. The registration response contains the user's ID, name and email.

## Sessions and Protected Access

Login receives an email and password through LoginRequest.

After a successful login, the server stores the user ID in a session. The client receives a session cookie that must be included in later requests.

The /api/auth/me endpoint checks the session and returns an unauthorized response when the user is not logged in.

## Reflection

Implementing these endpoints helped me understand the connection between request bodies, DTOs, database operations and HTTP responses.

A successful login must also lead to a way of identifying the user in subsequent requests. In this implementation, that is handled through a server-side session.