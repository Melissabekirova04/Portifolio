
---
title: "Week 8 – Security"
date: 2026-10-07
draft: false
summary: "Understanding backend security through password hashing, session-based authentication and protected resources in Travel Planner."
---

## 1. Topic and Project Connection

During the eighth week, we focused on security and how backend applications can protect user accounts and personal information.

Security is an important part of my Travel Planner project because users can create accounts, log in and save their personal travel plans.

The backend must ensure that passwords are stored securely and that users cannot access another user's saved trips.

In Travel Planner, I work with security through password hashing, session-based authentication, input validation and protected endpoints.

These features help protect user information and control access to the application's functionality.

## 2. Security in Backend Development

Backend security involves protecting data, verifying user identities and controlling access to resources.

Some important security concepts include:

- **Authentication:** Verifying who a user is.
- **Authorization:** Determining which resources an authenticated user can access.
- **Password hashing:** Storing passwords as secure hashes instead of plain text.
- **Session management:** Maintaining the user's authenticated state across HTTP requests.
- **Input validation:** Checking incoming data before processing or storing it.
- **Access control:** Preventing users from accessing resources they do not have permission to use.

Authentication and authorization are different concepts.

Authentication confirms the user's identity, while authorization determines what that user is allowed to do.

Both are important in Travel Planner because users must log in before accessing their personal trips.

## 3. How I Use Security in Travel Planner

### Password Hashing

When a user registers, the backend receives their name, email and password.

Instead of storing the password directly in PostgreSQL, I use the Password4j library with bcrypt to hash it.

The database stores the password hash rather than the original password.

During login, the submitted password is checked against the stored hash.

This protects user passwords by avoiding plain-text password storage.

### Session-Based Authentication

Travel Planner uses server-side sessions to maintain authentication.

The authentication process works as follows:

1. The user sends their email and password to the login endpoint.
2. The backend validates the credentials.
3. After successful authentication, the user's ID is stored in the session.
4. The client receives a session cookie.
5. The session cookie is included in later requests.
6. The backend uses the session to identify the authenticated user.

This allows users to remain authenticated across multiple HTTP requests without submitting their password each time.

### Protected REST Endpoints

I use authentication checks to protect endpoints that require a logged-in user.

For example:

- `POST /api/auth/register` – Creates a user account.
- `POST /api/auth/login` – Authenticates a user.
- `POST /api/auth/logout` – Ends the authenticated session.
- `GET /api/auth/me` – Returns the authenticated user's ID.
- `POST /api/trips` – Saves a trip for the logged-in user.
- `GET /api/trips` – Retrieves the logged-in user's saved trips.

If an unauthenticated user tries to access a protected endpoint, the backend returns HTTP 401 Unauthorized.

This prevents access to protected functionality without a valid session.

### Authorization and User Data

In Travel Planner, each saved trip is associated with a user through `user_id`.

When saving or retrieving trips, the backend uses the user ID from the authenticated session.

This is important because the backend should not rely on a user ID supplied by the client to decide who owns a trip.

By using the authenticated user's ID, the application can restrict access to that user's own travel information.

### Input Validation

Input validation is another part of the application's security.

During registration, the backend validates information such as:

- Required fields.
- Email format.
- Unique email addresses.

Validation helps prevent invalid data from being stored and provides appropriate responses when a request does not meet the requirements.

## 4. Reflection

During this week, I gained a better understanding of how security is implemented in a Java backend.

One of the most important things I learned was the difference between authentication and authorization.

A user being logged in does not automatically mean they should have access to every resource in the application.

I also learned why passwords should be hashed before being stored in a database and how bcrypt can be used for password verification.

Working with session-based authentication helped me understand how a backend can identify users across multiple HTTP requests.

In Travel Planner, connecting saved trips to the authenticated user is especially important because personal travel information should not be accessible to other users.

Security also needs to be considered when developing new features, rather than being added only at the end of a project.

This week helped me understand how password protection, authentication, authorization and validation work together to make a backend application more secure.
