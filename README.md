# Travel Planner

**Semesterprojekt – 3. semester, Datamatikeruddannelsen**

## 1. Introduktion

At planlægge en rejse handler om meget mere end blot at vælge en destination. Flybilletter skal sammenlignes, hoteller skal findes, budgettet skal overholdes, og der skal være plads til spændende oplevelser undervejs.

Det var udgangspunktet for udviklingen af **Travel Planner**.

Travel Planner er en backendbaseret rejseplanlægningsapplikation, som jeg har udviklet som mit individuelle semesterprojekt på 3. semester af datamatikeruddannelsen.

Formålet med projektet er at samle de vigtigste dele af rejseplanlægningen i ét system. Brugeren kan søge efter destinationer, undersøge flymuligheder, finde hoteller, udforske seværdigheder og oprette sin egen rejseplan med overblik over de forventede udgifter.

Projektet er udviklet i Java med Javalin som REST-framework og PostgreSQL som database. Derudover integrerer systemet eksterne API'er til flysøgning, vejroplysninger og destinationsinformation.

En central del af udviklingen har været at få forskellige teknologier til at kommunikere med hinanden. Jeg har derfor arbejdet med REST API'er, databasearkitektur, brugerautentificering, validering, fejlhåndtering, automatiserede tests og deployment.

Travel Planner er udviklet som et backendprojekt og foretager ikke faktiske fly- eller hotelbookinger.

## 2. Links

- **Portfolio-hjemmeside:** https://melissabekirova04.github.io/Portifolio/
- **GitHub – Portfolio:** https://github.com/Melissabekirova04/Portifolio
- **Deployet backend på DigitalOcean:** http://157.230.115.167

Backendens adresse giver adgang til API'et. Projektet har endnu ikke en selvstændig grafisk brugerflade.

## 3. Projektets funktioner

Travel Planner understøtter flere funktioner, som tilsammen danner grundlaget for en personlig rejseplan.

### Brugerregistrering og login
Brugeren kan oprette en konto, logge ind og få adgang til sine egne gemte rejser. Adgangskoder gemmes som hashes, og beskyttede endpoints anvender autentificering og adgangskontrol.

### Destinationssøgning
Brugeren kan søge efter destinationer ved hjælp af by- eller landenavne og vælge en destination til sin rejse.

### Destinationsinformation og inspiration
Systemet præsenterer information om destinationer, herunder beskrivelser, billeder og inspiration til oplevelser.

### Oprettelse af rejser
Brugeren kan oprette en rejse med destination, afrejsedato, hjemrejsedato og antal rejsende. Systemet validerer oplysningerne for at forhindre ugyldige rejseplaner.

### Flysøgning
Flytilbud hentes gennem Duffel API. Brugeren kan søge efter fly ud fra destination, datoer og antal rejsende samt angive, om der ønskes direkte fly.

### Hoteller
Brugeren kan undersøge hotelmuligheder og tilføje et valgt hotel til rejseplanen. Hoteltilbuddene er baseret på testdata og bruges til at demonstrere søgning, valg og prisberegning.

### Oplevelser og aktiviteter
Travel Planner giver inspiration til seværdigheder og aktiviteter. Brugeren kan filtrere anbefalinger efter interesser og tilføje aktiviteter til sin rejse.

### Rejseplan og budget
Systemet samler rejseoplysninger, valgte fly, hotel og aktiviteter i en rejseplan. Derudover beregnes et budget ud fra de registrerede udgifter.

### Vejroplysninger
Brugeren kan hente vejroplysninger for destinationer gennem Open-Meteo og få inspiration til aktiviteter ud fra vejret.

## 4. Anvendte teknologier

| Teknologi | Anvendelse |
|---|---|
| Java | Programmeringssprog til backend |
| Javalin 6.7.0 | REST API og HTTP-server |
| Maven | Projektstruktur og dependency management |
| Hibernate/JPA | Object-relational mapping |
| PostgreSQL | Relationel database |
| Jackson | Håndtering af JSON |
| Password4j | Hashing og verificering af adgangskoder |
| JWT | Tokenbaseret autentificering |
| Duffel API | Flysøgning |
| Open-Meteo API | Vejroplysninger |
| Wikipedia/Wikimedia | Destinationsinformation og billeder |
| JUnit | Automatiserede tests |
| Docker | Containerisering |
| GitHub Actions | Automatiserede workflows |
| Ubuntu | Operativsystem på serveren |
| DigitalOcean | Hosting og deployment |

## 5. Systemarkitektur

### System Overview

Travel Planner er udviklet med en lagdelt arkitektur, hvor hver del af applikationen har sit eget ansvarsområde.

**Controller**

Controllerlaget modtager HTTP-requests, behandler input, kalder relevante services og returnerer JSON samt HTTP-statuskoder.

**Service**

Servicelaget indeholder applikationens forretningslogik og håndterer blandt andet kommunikation med eksterne API'er.

**DAO (Data Access Object)**

DAO-laget håndterer kommunikationen med PostgreSQL gennem Hibernate/JPA. Her udføres databaseoperationer som oprettelse, hentning og opdatering af data.

**Entity**

Entityklasserne repræsenterer databasens tabeller og relationer.

**Security**

Sikkerhedslaget håndterer autentificering og adgangskontrol, så brugere kun kan tilgå de ressourcer, de har tilladelse til.

### Arkitekturdiagram

```text
Klient (curl / Postman / fremtidig frontend)
                    |
                 HTTP/JSON
                    |
              Javalin REST API
                    |
                Controllers
                    |
           -------------------
           |                 |
        Services          Security
           |                 |
           DAO         Autentificering
           |
      Hibernate/JPA
           |
       PostgreSQL

Services
   |
   |--- Duffel API
   |--- Open-Meteo API
   |--- Wikipedia/Wikimedia
```

### Distribueret kommunikation

Travel Planner består af flere komponenter, som kommunikerer gennem definerede grænseflader.

Klienten sender HTTP-requests til backendens REST API, som behandler forespørgslerne og returnerer JSON.

Backenden kommunikerer med PostgreSQL for at gemme og hente data samt med eksterne API'er for at indsamle oplysninger om fly, vejr og destinationer.

Dette gør Travel Planner til et eksempel på et distribueret system, hvor flere selvstændige tjenester samarbejder om at levere applikationens funktionalitet.

## 6. Vigtige designvalg

### Lagdelt arkitektur

**Valg:** Applikationen er opdelt i Controller, Service, DAO og Entity.

**Begrundelse:** Opdelingen adskiller HTTP-håndtering, forretningslogik og databaseadgang. Det gør koden mere overskuelig og lettere at vedligeholde og teste.

**Afvejning:** Arkitekturen kræver flere klasser og en tydelig struktur, men gør det nemmere at ændre eller udvide enkelte komponenter.

### Hibernate/JPA og PostgreSQL

**Valg:** PostgreSQL anvendes som relationsdatabase, mens Hibernate/JPA håndterer koblingen mellem Java-objekter og databasetabeller.

**Begrundelse:** Brugere, rejser, fly, hoteller, budgetter og aktiviteter har relationer, som passer til en relationel datamodel.

**Afvejning:** Relationerne gør det muligt at organisere data struktureret, men ændringer i datamodellen kan kræve tilpasninger i både entities, DAO'er og databasen.

### Autentificering og adgangskontrol

**Valg:** Travel Planner anvender autentificering til at beskytte brugeroplysninger og gemte rejser. Password4j anvendes til hashing og verificering af adgangskoder.

**Begrundelse:** Brugerens adgangskode skal ikke gemmes i klartekst, og personlige rejseoplysninger skal kun kunne tilgås af den rette bruger.

**Afvejning:** Sikkerhed kræver håndtering af tokens eller sessioner, validering af adgang og kontrol af ejerskab til data.

### Eksterne API'er

**Valg:** Travel Planner integrerer Duffel til flysøgning, Open-Meteo til vejrdata og Wikipedia/Wikimedia til destinationsinformation og billeder.

**Begrundelse:** Eksterne API'er gør det muligt at hente flyoplysninger, vejrudsigter og destinationsinformation uden at skulle opbygge og vedligeholde alle data lokalt.

**Afvejning:** Systemet afhænger af de eksterne tjenesters svartider, tilgængelighed og dataformater. Derfor er validering, fejlhåndtering og håndtering af manglende data vigtige dele af implementeringen.

### Validering og fejlhåndtering

**Valg:** API'et validerer brugerinput og returnerer relevante HTTP-statuskoder.

**Begrundelse:** Ugyldige oplysninger skal opdages, før de behandles eller gemmes i databasen.

**Afvejning:** Validering kræver ekstra kode og tests, men forbedrer systemets stabilitet og gør fejl lettere at identificere.

### Deployment med Docker

**Valg:** Backend og database er deployet på en DigitalOcean-server med Docker.

**Begrundelse:** Docker gør det muligt at køre applikationen og databasen i separate containere med hver deres konfiguration.

**Afvejning:** Deployment kræver håndtering af miljøvariabler, containere, netværk, databaseforbindelser og serverkonfiguration.

## 7. Datamodel

### ER-diagram (Entity-Relationship Diagram)

Mit ER-diagram kan findes i mappen **`docs`** under filnavnet `TravelPlanner-ERD.png`.

Diagrammet er udarbejdet i pgAdmin og viser databasens vigtigste tabeller samt relationerne mellem brugere, rejser, fly, hoteller, budgetter og aktiviteter.

![Travel Planner ER-diagram](docs/TravelPlanner-ERD.png)

### Centrale entiteter

| Entitet/tabel | Formål | Relation |
|---|---|---|
| User / users | Gemmer registrerede brugere | Én bruger kan have flere rejser |
| Trip / trips | Gemmer rejseoplysninger | Tilhører én bruger |
| TripFlight / trip_flights | Gemmer valgte flyoplysninger | Knyttet til en rejse |
| TripHotel / trip_hotels | Gemmer hoteloplysninger | Knyttet til en rejse |
| TripBudget / trip_budgets | Gemmer budgetoplysninger | Knyttet til en rejse |
| Activity / activities | Gemmer aktiviteter | Flere aktiviteter kan tilhøre én rejse |
| trip_hotel_amenities | Gemmer hotelfaciliteter | Knyttet til hoteloplysninger |

Databasen indeholder desuden tabellen `app_migrations`, som anvendes til registrering af databaseændringer.

### Designovervejelser

Datamodellen er blevet udvidet gennem projektets udvikling.

I begyndelsen blev flere rejseoplysninger håndteret samlet. Senere blev datamodellen opdelt i separate tabeller for blandt andet fly, hoteller og budgetter.

Denne opdeling gør det lettere at organisere data og arbejde med relationer, men kræver samtidig, at ændringer i databasen bliver håndteret korrekt i applikationens øvrige lag.

## 8. API-dokumentation

Travel Planner anvender et REST API, hvor data udveksles i JSON-format.

API'et er opdelt efter funktionalitet og håndterer blandt andet brugerautentificering, rejser, fly, hoteller, aktiviteter og vejroplysninger.

### Authentication

| Metode | Endpoint | Formål |
|---|---|---|
| POST | `/api/auth/register` | Opret en bruger |
| POST | `/api/auth/login` | Log ind |
| POST | `/api/auth/logout` | Log ud |
| GET | `/api/auth/me` | Hent oplysninger om den aktuelle bruger |

**Eksempel på registrering**

```http
POST /api/auth/register
Content-Type: application/json
```

Ved vellykket registrering returnerer API'et statuskoden `201 Created`.

### Trips

| Metode | Endpoint | Formål |
|---|---|---|
| POST | `/api/trips` | Opret og gem en rejse |
| GET | `/api/trips` | Hent brugerens gemte rejser |
| GET | `/api/trips/{id}` | Hent en bestemt rejse |

**Eksempel på rejseoplysninger**

```json
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
```

Eksemplet viser udvalgte oplysninger fra en rejse.

### Flights

| Metode | Endpoint | Formål |
|---|---|---|
| POST | `/api/trip-draft/flight` | Søg efter flytilbud til en rejsekladde |

Flysøgningen integrerer Duffel API og anvender rejsekriterier som destination, datoer og antal rejsende.

### Hotels

Hotelmodulet håndterer søgning efter hoteltilbud samt valg og lagring af hoteloplysninger på en rejse.

Hoteltilbuddene anvender fiktive testdata, som gør det muligt at demonstrere funktionaliteten uden en ekstern hotelbookingstjeneste.

### Destinations and Experiences

Destinationsmodulet understøtter søgning efter destinationer og hentning af inspirationsmateriale.

Oplevelser kan filtreres efter kategorier, og relevante aktiviteter kan tilføjes til en rejseplan.

Et eksempel på et endpoint til kategorifiltrering er:

```http
GET /api/destinations/OSL/experiences?categories=sevaerdigheder
```

### Weather

Vejrmodulet integrerer Open-Meteo og gør det muligt at hente vejroplysninger for destinationer.

Et eksempel på et endpoint til vejrafhængige anbefalinger er:

```http
GET /api/weather/destinations/OSL/recommendations
```

### HTTP-statuskoder

API'et anvender HTTP-statuskoder til at vise resultatet af en forespørgsel.

| Statuskode | Betydning |
|---|---|
| 200 OK | Forespørgslen blev gennemført |
| 201 Created | En ressource blev oprettet |
| 400 Bad Request | Ugyldigt input |
| 401 Unauthorized | Manglende eller ugyldig autentificering |
| 404 Not Found | Ressourcen blev ikke fundet |
| 409 Conflict | Konflikt med eksisterende data |
| 502 Bad Gateway | Problem ved kommunikation med en ekstern tjeneste |

## 9. User Stories

Projektets funktionalitet er udviklet med udgangspunkt i følgende user stories.

| US | Funktionalitet |
|---|---|
| US1 | Opret konto og login |
| US2 | Søg efter destinationer |
| US3 | Se destinationsinformation og inspiration |
| US4 | Opret rejse og angiv rejseoplysninger |
| US5 | Søg efter fly |
| US6 | Sammenlign flyafgange |
| US7 | Gem fly på rejsen |
| US8 | Søg efter hoteller |
| US9 | Sammenlign og vælg hotel |
| US10 | Se anbefalede oplevelser |
| US11 | Filtrer inspiration efter interesser |
| US12 | Tilføj oplevelser til rejseplanen |
| US13 | Opret og administrer egne aktiviteter |
| US14 | Se samlet rejseplan og budget |
| US15 | Se vejrdata og relevante anbefalinger |

User stories har fungeret som grundlag for implementering, test og videreudvikling af projektet.

## 10. Test og kvalitetssikring

Der er arbejdet med både automatiserede tests og manuelle HTTP-tests for at kontrollere applikationens funktionalitet.

### Automatiserede tests

Projektet anvender JUnit til at teste centrale dele af backendapplikationen.

Testarbejdet omfatter blandt andet:

- Brugerregistrering og login
- Autentificering og adgangskontrol
- Validering af brugerinput
- Databaseoperationer
- Rejseoplysninger og relationer
- Fejlhåndtering
- Integration mellem applikationens komponenter

Ved den senest gennemgåede testkørsel bestod 54 tests.

### HTTP-tests

REST API'et er desuden blevet testet gennem HTTP-requests.

Testene har blandt andet omfattet:

- Oprettelse af brugere
- Login og logout
- Adgang med og uden autentificering
- Destinationssøgning
- Oprettelse og hentning af rejser
- Flysøgning
- Hotelsøgning
- Tilføjelse og fjernelse af hotel
- Oplevelser og aktiviteter
- Rejseplan og budget
- Vejroplysninger
- Ugyldige datoer og datatyper
- Manglende eller ugyldige oplysninger

Testene har været en vigtig del af udviklingsprocessen og har gjort det lettere at opdage fejl ved ændringer i applikationen.

## 11. Deployment

Travel Planner er deployet på en DigitalOcean-server med Ubuntu og Docker.

Applikationen og PostgreSQL-databasen kører i separate containere.

Denne opsætning gør det muligt at administrere backend og database hver for sig og giver et mere ensartet kørselsmiljø.

### DigitalOcean

Den deployede backend er tilgængelig på:

http://157.230.115.167

API'et kan testes ved at sende HTTP-requests til de relevante endpoints.

### Docker

Docker anvendes til at containerisere applikationen og databasen.

Containeropsætningen gør det lettere at håndtere afhængigheder og konfigurere applikationens kørselsmiljø.

### GitHub Actions

Projektet anvender GitHub Actions til automatiserede workflows i udviklingsprocessen.

Der er blandt andet arbejdet med Maven, Docker og deployment af Hugo-portfolioen.

## 12. Udviklingsproces

Travel Planner er udviklet gradvist gennem semesteret.

Projektet begyndte med brugerregistrering, login og grundlæggende databasefunktionalitet.

Derefter blev destinationssøgning og oprettelse af rejser implementeret.

Efterfølgende blev systemet udvidet med eksterne API'er til flysøgning, vejrdata og destinationsinformation samt funktioner til hoteller, aktiviteter og budget.

Undervejs blev datamodellen videreudviklet for at understøtte de nye funktioner.

Den sidste del af arbejdet har blandt andet fokuseret på automatiserede tests, fejlhåndtering, Docker og deployment på DigitalOcean.

### Udfordringer og læring

**Eksterne API'er**

Arbejdet med eksterne API'er har krævet forståelse for HTTP-kommunikation, JSON og håndtering af data fra forskellige tjenester.

**Databasedesign**

Udvidelsen af datamodellen har vist, hvordan ændringer i databasearkitekturen kan påvirke flere dele af applikationen.

**Sikkerhed**

Implementeringen af autentificering og adgangskontrol har givet erfaring med beskyttelse af brugeroplysninger og kontrol af adgang til personlige rejser.

**Deployment**

Opsætningen på DigitalOcean har givet praktisk erfaring med Docker, miljøvariabler, containere og databaseforbindelser.

**Test**

Arbejdet med automatiserede tests og HTTP-tests har gjort det lettere at kontrollere funktionalitet og opdage fejl under udviklingen.

## 13. Lokal opsætning

### Forudsætninger

For at køre projektet lokalt kræves:

- Java
- Maven
- PostgreSQL
- Relevante miljøvariabler
- API-nøgler til de integrationer, der kræver adgang

### Opsætning

1. Klon projektets backend-repository fra GitHub.
2. Opret en PostgreSQL-database til Travel Planner.
3. Konfigurer databaseforbindelsen.
4. Tilføj de nødvendige miljøvariabler til databaseadgang, autentificering og eksterne API'er.
5. Start applikationen gennem projektets main-klasse eller Maven.
6. Test REST API'et med eksempelvis IntelliJ HTTP Client, Postman eller curl.

Under lokal udvikling har API'et været tilgængeligt på:

```text
http://localhost:7070
```

### Kør tests

Projektets automatiserede tests kan køres gennem IntelliJ eller Maven.

```bash
mvn test
```

For at gennemføre Maven-build og verificering:

```bash
mvn clean verify
```

Adgangskoder, databaseoplysninger og API-nøgler skal opbevares som miljøvariabler og må ikke offentliggøres i GitHub-repositoryet.

## 14. Mulig videreudvikling

Travel Planner er udviklet som et backendprojekt.

En naturlig videreudvikling vil være at udvikle en frontend, hvor brugeren kan planlægge og administrere sine rejser gennem en grafisk brugerflade.

Andre muligheder for videreudvikling er:

- Integration af live hoteldata
- Udvidelse af destinationsudvalget
- Mere detaljeret budgetstyring
- Flere kategorier af aktiviteter
- Personlige anbefalinger baseret på brugerens interesser
- Forbedret visualisering af rejseplanen

## 15. Konklusion

Travel Planner har været et projekt, hvor jeg har kunnet samle og anvende flere af de teknologier og metoder, jeg har arbejdet med gennem datamatikeruddannelsen.

Projektet har givet mig erfaring med at udvikle en backendapplikation med REST API, relationsdatabase, autentificering, eksterne API'er, automatiserede tests og deployment.

Særligt arbejdet med at integrere flere selvstændige komponenter har styrket min forståelse af backendudvikling og distribuerede systemer.

Resultatet er en backendapplikation, der danner grundlaget for et samlet rejseplanlægningssystem og kan videreudvikles med en frontend og yderligere funktionalitet.