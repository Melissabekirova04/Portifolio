---
title: "Week 5 – SP1 & Project Structure"
weight: 3
draft: false
summary: "Connecting the semester topics to the structure and scope of Travel Planner."
---

## Topic and Project Connection

SP1 appears in the teaching plan for this week. This entry focuses on how the semester topics connect to my ongoing Travel Planner project.

Travel Planner is a backend project. Its current features include weather information and account registration and login.

## User Stories

User stories help me describe the application from the user's perspective.

One central user story is:

“As a user, I want to create an account and log in so that I can save and manage my personal trips.”

The acceptance criteria include registration with a name, email and password, a valid and unique email address, and access to protected functionality after login.

Account registration and login provide the foundation for this story. Saving and managing trips still requires additional functionality.

## Project Structure

The backend is divided into packages for configuration, controllers, DAOs, DTOs, entities and services.

This makes it easier to locate code and understand its responsibility. Main starts the Javalin server and registers the controllers.

## Reflection

Breaking the project into smaller features helps me focus on one part at a time.

A working account system is useful progress, but it does not complete the full travel-planning functionality. The next stages need to connect users with their own saved trips.