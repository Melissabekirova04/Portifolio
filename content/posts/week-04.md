
---
title: "Week 4 – Concurrency"
weight: 4
draft: false
summary: "Understanding concurrency, simultaneous requests and database resource management in Travel Planner."
---

## 1. Topic and Project Connection

During the fourth week, we focused on concurrency and how applications can handle multiple tasks or requests at the same time.

Concurrency is relevant to my Travel Planner backend because the server may receive requests from several users simultaneously.

For example, one user might be searching for flights while another user is retrieving weather information or accessing saved trips.

Understanding concurrency is important for developing a backend that can handle multiple users without causing problems with shared resources.

## 2. Concurrency in Backend Development

Concurrency means that multiple tasks can make progress during the same period of time.

In a backend application, concurrency is important because several HTTP requests may need to be processed at once.

Some important concepts include:

- **Threads:** Allow tasks to execute independently.
- **Thread safety:** Ensures that shared resources are accessed safely.
- **Race conditions:** Occur when the result depends on the timing of concurrent operations.
- **Shared resources:** Resources such as database connections or shared application data that require careful management.

Concurrency can improve responsiveness, especially when tasks spend time waiting for external API responses.

However, it can also introduce additional complexity and make errors more difficult to identify.

## 3. How Concurrency Relates to Travel Planner

In Travel Planner, concurrency is relevant in two areas.

**Handling multiple user requests**

The Javalin backend can receive HTTP requests from multiple users.

For example:
- One user can log in.
- Another user can retrieve saved trips.
- A third user can request weather information.

These requests should be handled without interfering with each other's data.

**Database operations with Hibernate**

Hibernate uses an `EntityManager` to interact with PostgreSQL.

An `EntityManager` is not thread-safe and should therefore not be shared between concurrent requests.

In my project, the DAO creates an `EntityManager` for each database operation and closes it when the operation is finished.

This helps keep database operations separate and reduces the risk of concurrency-related problems.

**Possible concurrent weather requests**

One possible application of concurrency would be retrieving weather information for multiple cities at the same time.

Instead of waiting for one external API request to finish before starting the next, requests could be performed concurrently.

This functionality has not been implemented in Travel Planner.

It would require handling multiple tasks, limiting simultaneous requests and managing situations where one or more external API calls fail.

## 4. Reflection

During this week, I learned why concurrency is important when developing backend applications that may serve multiple users.

I gained a better understanding of threads, shared resources and the problems that can occur when several operations access the same data.

One important lesson was that concurrency is not only about improving performance. It is also about ensuring that requests and database operations are handled safely.

In Travel Planner, managing Hibernate's `EntityManager` correctly is especially important because database operations may happen while multiple users are interacting with the backend.

Although I have not implemented concurrent weather fetching, this topic helped me understand how concurrency could be introduced and what challenges would need to be considered.

Before implementing concurrent API requests, I would need to evaluate whether they improve performance and how errors and resource usage should be handled.
