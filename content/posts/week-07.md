
---
title: "Week 7 – REST & Test"
weight: 1
draft: false
summary: "Testing REST endpoints, HTTP responses and session-based authentication in Travel Planner."
---

## 1. Topic and Project Connection

During the seventh week, we focused on REST and testing, including how to verify that backend endpoints behave as expected.

Testing is an important part of backend development because an API must return the correct responses and handle both valid and invalid requests.

In my Travel Planner project, I worked with manually testing the authentication endpoints using curl.

This allowed me to send HTTP requests directly to my Javalin backend and inspect the responses.

At this stage, I focused on verifying that registration, login and session-based authentication worked correctly.

## 2. REST API Testing

REST API testing involves sending HTTP requests to endpoints and checking whether the responses match the expected behaviour.

Important aspects of API testing include:

- **HTTP methods:** Verifying that endpoints respond correctly to requests.
- **Status codes:** Checking whether the server returns the expected HTTP status.
- **Response bodies:** Verifying that the returned JSON contains the expected information.
- **Authentication:** Checking whether protected endpoints require a valid login.
- **Error handling:** Ensuring that invalid requests receive appropriate responses.

I used curl, a command-line tool that can send HTTP requests to a server.

Using curl allowed me to test the backend without needing a frontend application.

This was useful because Travel Planner is primarily a backend project.

## 3. How I Test REST Endpoints in Travel Planner

I manually tested the authentication functionality through the REST endpoints implemented with Javalin.

### Registration

**Endpoint:** `POST /api/auth/register`

I tested whether the backend could register a new user.

The test confirmed that:

- The endpoint returned HTTP 201 Created.
- The response contained the user's ID, name and email.
- The registration process completed successfully.

### Login

**Endpoint:** `POST /api/auth/login`

I tested whether an existing user could log in using their email and password.

The test confirmed that:

- The endpoint returned HTTP 200 OK.
- The backend created an authenticated session.
- The response included a session cookie.

### Protected Endpoint

**Endpoint:** `GET /api/auth/me`

I tested the endpoint both with and without an authenticated session.

The results were:

- Without a session: HTTP 401 Unauthorized.
- With a valid session cookie: The endpoint returned the authenticated user's ID.

This confirmed that the backend could distinguish between authenticated and unauthenticated requests.

### Testing Session Cookies with curl

To test authenticated requests, I used curl to save the session cookie and reuse it in later requests.

For example:

```bash
curl -i -b /tmp/travelplanner-cookies.txt \
  http://localhost:7070/api/auth/me
```

The `-i` option includes the HTTP response headers.

The `-b` option sends cookies stored in the specified file.

This allowed me to verify that the backend recognized the user across multiple HTTP requests.

### Further Test Cases

After verifying the successful authentication flow, I identified additional scenarios that should be tested:

- Registering with an email address that already exists.
- Registering with an invalid email address.
- Registering without required fields.
- Logging in with an incorrect password.
- Accessing protected endpoints after logout.

These cases are important because testing only successful requests does not verify whether the backend handles errors and unauthorized access correctly.

## 4. Reflection

During this week, I gained a better understanding of how REST endpoints can be tested independently of a frontend application.

Using curl helped me understand how HTTP requests, status codes, JSON responses and session cookies work together.

One important learning experience was testing authentication across multiple requests.

I learned that a successful login is not enough on its own. The backend must also recognize the authenticated user when accessing protected endpoints.

I also learned the importance of testing both successful and unsuccessful scenarios.

The manual tests confirmed that the basic registration and session flow worked, but they were not automated.

A next step was to introduce automated tests that could be executed repeatedly to verify endpoint behaviour and database operations.

This week helped me understand how testing contributes to building a more reliable and secure backend application.
