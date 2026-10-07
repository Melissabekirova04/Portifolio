---
title: "Week 4 – Concurrency"
weight: 4
draft: false
summary: "Considering concurrent requests and shared resources in the backend."
---

## Topic and Project Connection

Concurrency concerns work that can happen at the same time.

This is relevant to Travel Planner because the server can receive requests from multiple users. Fetching weather for several cities also involves multiple external API requests.

## Possible Use in Travel Planner

One possible improvement is to fetch weather for different airport cities concurrently.

Requests to an external service spend time waiting for responses. Concurrent requests may reduce the total waiting time compared with fetching each city's weather sequentially.

This would require limits on the number of simultaneous requests and a clear way to handle partial failures.

## Database Considerations

Concurrency also matters when working with Hibernate. An EntityManager should not be shared between requests running at the same time.

The DAO creates an EntityManager for each database operation and closes it afterwards.

## Reflection and Next Steps

I have not implemented concurrent weather fetching in Travel Planner yet.

Before adding it, I need to understand how to manage tasks, collect their results and handle errors. I also need to measure whether the change actually improves response time.