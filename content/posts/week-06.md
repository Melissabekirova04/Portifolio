
---
title: "Week 6 – REST I"
weight: 2
draft: false
summary: "Understanding REST APIs, HTTP methods and implementing user authentication with Javalin in Travel Planner."
---

## 1. Topic and Project Connection

During the sixth week, we focused on REST and how clients communicate with backend applications through HTTP.

REST (Representational State Transfer) is an architectural style used when designing web APIs.

In my Travel Planner project, I use Javalin to create REST endpoints that allow clients to communicate with my Java backend.

At this stage, I focused on implementing user registration, login, logout and protected endpoints.

These features are important because users need to authenticate before accessing their personal travel information.

## 2. REST APIs and HTTP Methods

A REST API allows clients to interact with a backend through HTTP requests.

Different HTTP methods are used depending on the operation:

- **GET:** Retrieves information from the server.
- **POST:** Sends data to the server, often to create resources or perform an action.
- **PUT:** Replaces or updates an existing resource.
- **PATCH:** Partially updates an existing resource.
- **DELETE:** Removes a resource.

REST APIs commonly use JSON to exchange data between the client and server.

HTTP status codes indicate the result of a request.

Examples include:

- **200 OK:** The request was successful.
- **201 Created:** A new resource was created.
- **400 Bad Request:** The request contains invalid data.
- **401 Unauthorized:** Authentication is required or has failed.
- **409 Conflict:** The request conflicts with existing data.
- **500 Internal Server Error:** An unexpected server error occurred.

In Travel Planner, these concepts are used when handling registration, login and access to protected functionality.

## 3. How I Use REST in Travel Planner

I implemented authentication endpoints using Javalin.

### Registration

**Endpoint:** `POST /api/auth/register`

The endpoint receives registration information through a `RegisterRequest` DTO.

The request contains:

- Name
- Email
- Password

The backend validates the input and checks whether the email address is already registered.

Before storing the user in PostgreSQL, the password is hashed using bcrypt through the Password4j library.

The registration response contains the user's ID, name and email without exposing the password hash.

### Login

**Endpoint:** `POST /api/auth/login`

The login endpoint receives an email address and password through a `LoginRequest` DTO.

The backend checks the submitted credentials against the stored user information.

When authentication succeeds, the user's ID is stored in a server-side session.

The client receives a session cookie, which can be included in later requests.

### Logout

**Endpoint:** `POST /api/auth/logout`

The logout endpoint ends the user's authenticated session.

This prevents the previous session from continuing to provide authenticated access.

### Protected Endpoint

**Endpoint:** `GET /api/auth/me`

This endpoint checks whether the client has an authenticated session.

If the user is logged in, the endpoint returns information identifying the authenticated user.

If the user is not authenticated, the backend returns an HTTP 401 Unauthorized response.

### Application Structure

The authentication functionality connects several backend components:

1. The client sends an HTTP request to a Javalin endpoint.
2. The controller reads the request data using a DTO.
3. The backend validates the input and processes the authentication logic.
4. The DAO communicates with PostgreSQL through Hibernate/JPA.
5. The controller returns an HTTP response to the client.

This structure separates HTTP communication from database persistence.

## 4. Reflection

During this week, I gained a better understanding of how REST APIs allow clients to communicate with backend applications.

Implementing registration and login helped me understand the relationship between HTTP methods, endpoints, JSON request bodies, DTOs and database operations.

I also learned that authentication involves more than verifying a password.

After a successful login, the backend must be able to identify the user in subsequent requests.

In Travel Planner, I use server-side sessions and cookies to maintain authentication between requests.

Another important lesson was the use of HTTP status codes to communicate whether a request succeeded or failed.

This week gave me practical experience with designing REST endpoints and implementing authentication, which provides the foundation for protecting personal trip information later in the project.
