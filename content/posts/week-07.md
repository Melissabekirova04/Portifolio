---
title: "Week 7 – REST & Test"
weight: 1
draft: false
summary: "Manually testing authentication and identifying the next test cases."
---

## Topic and Project Connection

The current topic is REST and testing.

For Travel Planner, I have started by manually testing the authentication endpoints with curl. This allows me to inspect the HTTP status, response body and session cookie.

## Verified Behaviour

The manual checks confirmed that:

- Registration returned 201 Created with the user's ID, name and email.
- Login returned 200 OK and a session cookie.
- Accessing /api/auth/me without a session returned 401 Unauthorized.
- Accessing /api/auth/me with the saved session cookie returned the user ID.

To reuse the login session, curl saves the cookie in a file and sends it with the next request:

```bash
curl -i -b /tmp/travelplanner-cookies.txt \
  http://localhost:7070/api/auth/me
```

## Further Test Cases

The next checks should cover:

- Registration with an email that already exists.
- Registration with an invalid email.
- Missing required fields.
- Login with an incorrect password.
- Access to the protected endpoint after logout.

These cases are important because successful requests alone do not show whether validation and access control behave correctly.

## Reflection

The manual tests provide evidence that the basic registration and session flow works.

They are not automated tests. A next step is to add repeatable tests that check expected responses and relevant database behaviour.