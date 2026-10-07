---
title: "Week 8 – Destination Search, Flights and Inspiration"
date: 2026-10-07
draft: false
---

This week, I worked on US2 and US3 in my Travel Planner backend. I added destination search, integrated flight offers, saved trips in PostgreSQL and added destination inspiration.

## US2 – Search for a destination

I created a catalogue of 12 destinations across Denmark, Norway and Sweden. Users can search by city, country or airport code, including Danish names such as “København” and “Norge”.

Users can select a destination and start a trip draft. The draft is stored in their session while they continue planning.

## Flight search with Duffel

I integrated the Duffel API to search for one-way economy flights. Users can specify their departure airport, destination, departure date and number of adults.

The API returns offers containing prices, airlines, flight segments and departure and arrival times. Users can choose an offer from their latest search, and the backend checks that it has not expired.

I used Duffel’s test environment. Selecting or saving an offer does not book a real flight.

## Saving trips in PostgreSQL

I added a Trip entity, a TripDAO and a TripController so logged-in users can save and retrieve their trips.

The trip is linked to the user through user_id. The backend uses the user ID from the login session when saving and retrieving trips.

The destination, search criteria and selected flight offer are stored as JSON text. This preserves the planning information, but separate database columns would make it easier to query individual details later.

I tested saving a destination on its own and saving a destination with a selected flight. Both could be retrieved through the API.

## US3 – Destination information and inspiration

I added an endpoint that returns a short description, three suggested areas and an image link for each destination.

The descriptions and areas are maintained in my own catalogue. Images are retrieved through Wikipedia’s API. If an image cannot be retrieved, the endpoint still returns the description and areas with an image status of “unavailable”.

Users can continue planning by selecting the destination through the existing trip-draft endpoint.

This completes the backend functionality for destination inspiration. A frontend will later display the images, descriptions and a button for starting a trip.

## Testing and troubleshooting

I tested the endpoints manually with curl. A cookie file preserved the login session between requests.

The tested flow included:

- Creating an account and logging in.
- Selecting Oslo as a destination.
- Searching for flights and selecting an offer.
- Saving the trip and retrieving it with the flight information.
- Retrieving Oslo’s description, suggested areas and image link.
- Starting a new trip draft after viewing destination inspiration.

During development, PostgreSQL was not running, which caused a connection error and prevented the application from starting. Starting the database resolved the problem.

I also received a 404 response when trying to save a trip. The cause was that TripController had not been registered in Main. Registering the controller and restarting the application made the endpoint available.

## Reflection

This week helped me understand how controllers, services and DAOs work together. Controllers handle requests, services provide destination information and external API integration, and DAOs handle database access.

I also gained experience with sessions, validation and error handling. Testing the complete flow helped me check that the features worked together, from choosing a destination to retrieving a saved trip.

The next step is to add more validation tests and continue developing the remaining user stories.