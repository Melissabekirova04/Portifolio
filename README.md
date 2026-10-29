Travel Planner
Vision
Travel Planner er en backendbaseret rejseplanlægningsapplikation, der er udviklet som et individuelt semesterprojekt på 3. semester af datamatikeruddannelsen. Formålet er at samle destinationssøgning, fly, hoteller og rejseplanlægning i et system, hvor brugeren kan gemme sine rejseoplysninger.
Projektet fokuserer på distribuerede systemer: en klient kommunikerer med et Java-baseret REST API, som arbejder med en PostgreSQL-database og integrerer eksterne tjenester. Projektet foretager ikke faktiske fly- eller hotelbookinger.

Links
GitHub – Travel Planner (backendprojekt): https://github.com/Melissabekirova04/Portifolio

Portfolio-hjemmeside: https://melissabekirova04.github.io/Portifolio/

Deployed-hjemmeside:

Architecture:
System Overview
Travel Planner er udviklet i Java med Javalin 6.7.0 som HTTP-server og REST-framework. Koden er opdelt efter ansvar:
•	Controller: Modtager HTTP-requests, læser og validerer input samt returnerer JSON og relevante HTTP-statuskoder.
•	Service: Indeholder forretningslogik og integrationer til eksterne API'er, blandt andet fly- og vejrtjenester.
•	DAO (Data Access Object): Håndterer lagring og hentning af data.
•	Entity: Repræsenterer datamodellen og dens relationer via Hibernate/JPA.
•	Security: Håndterer registrering, login og beskyttelse af relevante endpoints.
Anvendte teknologier: Java, Javalin, Maven, Hibernate/JPA, PostgreSQL, Jackson, Password4j, Duffel API, Open-Meteo, JUnit, Docker, Ubuntu og DigitalOcean.

Distribueret kommunikation:
Klient og backend udveksler data over HTTP, typisk i JSON-format. Backenden kommunikerer med PostgreSQL for vedvarende lagring og med eksterne API'er for data, som ikke vedligeholdes lokalt. Disse systemer er udviklet på forskellige platforme og kommunikerer gennem definerede grænseflader.
Systemet kan modtage forespørgsler fra flere brugere. Brugeridentitet og ejerskab til rejser er derfor vigtige hensyn, så data ikke blandes mellem brugere. Det er ikke det samme som at dokumentere belastningstest eller garantere en bestemt kapacitet; det ville kræve særskilte målinger.

Architecture Diagram:
Klient (curl / Postman / fremtidig frontend)
|
HTTP/JSON
|
Javalin REST API
|
Controllers
/       \
Services     Security
|             |
DAO / JPA     Login / session
|
PostgreSQL

Services ----HTTP----> Duffel API / Open-Meteo

Key Design Decisions

Lagdelt arkitektur:
Valg: Controller, Service, DAO og Entity har forskellige ansvarsområder.
Begrundelse: Det adskiller HTTP-håndtering, forretningslogik og persistence, så koden bliver nemmere at forstå, fejlfinde og teste.
Konsekvens: Flere klasser og mere struktur kræver ekstra arbejde, men gør det lettere at udskifte eller videreudvikle enkelte komponenter.

JPA/Hibernate og PostgreSQL:
Valg: En relationsdatabase med JPA-entities og DAO-lag.
Begrundelse: Brugere, rejser, fly, hoteller, budgetter og aktiviteter har tydelige relationer, som passer til en relationel datamodel.
Konsekvens: Datamodellen kan håndhæve relationer og understøtte vedvarende lagring. Til gengæld skal ændringer i entities og databaseskema holdes synkroniserede, og relationer kan give komplekse forespørgsler.

Sessionsbaseret autentificering:
Valg: Login håndteres med session/cookie, og adgangskoder hashes med Password4j.
Begrundelse: En session gør det muligt at identificere en bruger på tværs af API-kald uden at sende adgangskoden ved hver forespørgsel.
Konsekvens: Beskyttede endpoints skal kontrollere brugerens session og ejerskab til data. Ved deployment skal cookieindstillinger, HTTPS og eventuel skalering på tværs af flere serverinstanser vurderes. Projektet beskrives ikke som JWT-baseret, da den implementerede løsning bruger sessioner.

Eksterne API'er:
Valg: Flyoplysninger hentes via Duffel, og vejroplysninger via Open-Meteo.
Begrundelse: Eksterne tjenester leverer data, som det ville være uhensigtsmæssigt at opbygge og vedligeholde selv.
Konsekvens: Applikationen afhænger af netværksforbindelse, API-adgang, svartider og eksterne tjenesters datakvalitet. Fejl og manglende svar skal derfor håndteres tydeligt.

Fejlhåndtering og validering:
Valg: API'et validerer relevante brugerinput og anvender HTTP-statuskoder til at beskrive udfaldet af requests.
Begrundelse: Klienten skal kunne skelne mellem eksempelvis ugyldigt input, manglende adgang, ikke-fundne ressourcer og problemer med eksterne tjenester.
Konsekvens: Der skal testes både succes- og fejlscenarier. Konkrete statuskoder afhænger af det enkelte endpoint og dets implementering.

Deployment med Docker:
Valg: Backend og database er sat op på en DigitalOcean-server med Ubuntu og Docker.
Begrundelse: Containere gør det nemmere at styre applikationens kørselsmiljø og adskille backend og database.
Konsekvens: Deployment kræver håndtering af miljøvariabler, netværk, vedvarende databasevolumener, serveropdateringer og logning. En fungerende lokal applikation er ikke i sig selv garanti for korrekt drift på serveren.

Data Model:
ERD
ER-diagram: docs/TravelPlanner-ERD.png (indsættes fra projektmappen)
ER-diagrammet er lavet i pgAdmin og viser de vigtigste tabeller og deres relationer. Billedet skal ligge i docs/TravelPlanner-ERD.png i samme repository som denne README.

Important Entities:
Entity / tabel	Formål	Relation
User / users	Registreret bruger med loginoplysninger	Én bruger kan have flere rejser
Trip / trips	En gemt rejse med destination og øvrige rejseoplysninger	Tilhører én bruger
TripFlight / trip_flights	Valgt flytilbud til en rejse	Knyttet til en rejse
TripHotel / trip_hotels	Hoteloplysninger knyttet til rejsen	Knyttet til en rejse
TripBudget / trip_budgets	Budgetoplysninger	Knyttet til en rejse
Activity / activities	Aktiviteter på rejsen	Flere aktiviteter kan tilhøre én rejse
trip_hotel_amenities	Faciliteter knyttet til et hotel	Flere faciliteter kan tilhøre ét hotel

Derudover findes app_migrations til registrering af databaseændringer. Tabellen er teknisk og er ikke en del af brugerens rejseplan.
Designovervejelse: At opdele fly, hotel og budget i særskilte tabeller gør datamodellen mere overskuelig og understøtter relationer. Det medfører dog, at data nogle gange skal hentes gennem flere tabeller. Databasen indeholder også ældre rejsekolonner fra tidligere iterationer; en mulig videreudvikling er at rydde op i disse, når migrering og bagudkompatibilitet er afklaret.

API Documentation:
REST API'et udveksler JSON. Nedenstående er de centrale endpoints, der tidligere er implementeret eller testet i projektet. Den endelige oversigt bør kontrolleres mod de aktuelle route-definitioner i koden inden aflevering.
Authentication
Metode	Endpoint	Formål
POST	/api/auth/register	Opret bruger
POST	/api/auth/login	Log ind og opret session
POST	/api/auth/logout	Log ud
GET	/api/auth/me	Hent oplysninger om aktuel session/bruger

Registrering:
POST /api/auth/register
Content-Type: application/json
Et gyldigt registreringsrequest indeholder de brugeroplysninger, som controlleren kræver, herunder e-mail og adgangskode. Ved vellykket oprettelse returnerer API'et 201 Created.

Login og session:
POST /api/auth/login
Content-Type: application/json
Ved vellykket login anvendes en sessionscookie til efterfølgende requests. Det præcise JSON-format for request og response skal aflæses i den aktuelle controller, så dokumentationen matcher koden.

Trips:
Metode	Endpoint	Formål
POST	/api/trips	Opret og gem en rejse
GET	/api/trips	Hent brugerens rejser
GET	/api/trips/{id}	Hent en bestemt rejse

Eksempel på rejseoplysninger:
En gemt rejse kan blandt andet indeholde følgende destination og søgekriterier:
{
"destination": {
"city": "Oslo",
"country": "Norway",
"airportCode": "OSL"
},
"search": {
"origin": "CPH",
"destination": "OSL",
"departureDate": "2026-11-20",
"returnDate": "2026-11-25",
"adults": 1,
"directOnly": false
}
}
Eksemplet viser udvalgte felter fra en rejse, ikke nødvendigvis et komplet gyldigt POST /api/trips-request. Det præcise requestformat skal verificeres i TripController.

Flights:
POST /api/trip-draft/flight
Endpointet bruges til at søge efter flytilbud til en rejsekladde. Flysøgningen integrerer Duffel API og understøtter blandt andet rejsekriterier og ønske om direkte fly.

Weather, hotels and other functionality:
Projektet har desuden komponenter til vejr, hoteller, destinationer, budget og aktiviteter. De præcise URL'er og HTTP-metoder bør indsættes ud fra den seneste kode, frem for at dokumentere planlagte endpoints som færdigimplementerede.

Test af API'et:
API'et kan testes med eksempelvis curl eller Postman. Under lokal udvikling har backend kørt på:
http://localhost:7070
Enkelte endpoints kræver en aktiv login-session. På serveren er API'et tidligere testet via HTTP-requests, blandt andet registrering, login og hentning af rejser.

User Stories:
De centrale brugerbehov i Travel Planner omfatter
US1 – Opret konto og login: Oprette en konto og logge ind for at gemme og administrere personlige rejser.
US2 – Søg efter destination: Søge efter destinationer ved hjælp af by eller land.
US3 – Se destination og inspiration: Se beskrivelser, billeder og populære områder for en destination.
US4 – Opret rejse og vælg datoer: Oprette en rejse med destination, startdato, slutdato og budget.
US5 – Søg efter fly: Søge efter fly ud fra afrejselufthavn, destination og rejsedatoer.
US6 – Sammenlign flyafgange: Sammenligne fly ud fra blandt andet pris, flyselskab og rejsetid.
US7 – Gem fly på rejsen: Vælge og gemme flyafgange som en del af rejseplanen.
US8 – Søg efter hoteller: Søge efter hoteller ud fra destination og rejsedatoer.
US9 – Sammenlign og vælg hotel: Sammenligne hoteloplysninger og gemme et valgt hotel på rejsen.
US10 – Se anbefalede oplevelser: Få inspiration til seværdigheder og oplevelser på destinationen.
US11 – Filtrer inspiration efter interesser: Filtrere anbefalinger efter eksempelvis mad, natur og shopping.
US12 – Gem inspiration i rejseplanen: Tilføje anbefalede oplevelser til en personlig rejseplan.
US13 – Opret og administrer egne aktiviteter: Oprette, redigere og slette egne aktiviteter.
US14 – Se samlet itinerary og budget: Se rejseplanen dag for dag og få overblik over planlagte udgifter.
US15 – Se vejr og relevante forslag: Se vejroplysninger og få inspiration til aktiviteter ud fra vejret.
Status: User stories beskriver både gennemførte og planlagte funktioner. Listen er projektets målsætninger og må ikke forveksles med en liste over fuldt implementerede endpoints. De faktisk implementerede funktioner er beskrevet i API Documentation og øvrige tekniske afsnit.

Development Notes
Udviklingsproces:
Projektet er udviklet gradvist: først med brugerregistrering og login, derefter destinations- og rejsefunktioner, integration til eksterne API'er, udbygning af datamodellen, automatiserede tests og deployment.
Undervejs er datamodellen blevet udvidet fra mere samlede rejseoplysninger til særskilte tabeller for blandt andet fly, hotel og budget. Det har krævet justeringer i både entities, DAO'er og controllers.

Test og kvalitet:
Der er skrevet automatiserede tests til projektet. Ved den senest gennemgåede testkørsel bestod 54 tests. Det viser, at de dækkede scenarier fungerede på testtidspunktet, men erstatter ikke sikkerhedstest, belastningstest eller kontrol af hele produktionsmiljøet.

Udfordringer og læring:
•	Integration: Eksterne API'er bruger egne dataformater og kan fejle uafhængigt af backend. Det kræver konvertering af data og håndtering af fejl.
•	Databasedesign: Ændringer i relationer påvirker flere lag i applikationen og kræver omtanke ved migrering.
•	Autentificering: Det er nødvendigt at adskille identifikation af en bruger fra kontrollen af, hvilke rejser brugeren må tilgå.
•	Deployment: Opsætning på DigitalOcean med Docker kræver forståelse for miljøvariabler, containere, netværk og databaseforbindelser.
•	Test: Automatiserede tests hjælper med at opdage regressioner, når en eksisterende funktion ændres.

Lokal opsætning:
Forudsætninger: Java, Maven og PostgreSQL samt nødvendige API-nøgler til de integrationer, der ønskes testet.
1. Klon projektet fra GitHub: https://github.com/Melissabekirova04/Portifolio
2. Opret en lokal PostgreSQL-database og en separat testdatabase efter projektets konfiguration.
3. Opsæt de miljøvariabler, som projektets konfigurationsklasser læser (herunder database-login og relevante API-nøgler). De præcise variabelnavne skal verificeres i kildekoden.
4. Start applikationen gennem projektets main-klasse eller Maven-opsætning.
5. Test API'et på den port, der er angivet i konfigurationen (tidligere 7070 lokalt).
6. Kør testene via projektets Maven-testopsætning.
   Sikkerhed: Adgangskoder, databasecredentials og API-nøgler må ikke lægges i README'en eller pushes til GitHub.

