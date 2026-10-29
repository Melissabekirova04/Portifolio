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

Travel Planner tilbyder et REST API, hvor requests og responses som udgangspunkt bruger JSON. De følgende **43 endpoints** er registreret i projektets 10 controllers. Eksemplerne viser udvalgte felter; de er ikke en fuldstændig specifikation af alle JSON-responses.

### Authentication (4 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| POST | `/api/auth/register` | Registrer bruger (`201 Created`) |
| POST | `/api/auth/login` | Log ind og modtag JWT (`200 OK`) |
| GET | `/api/auth/me` | Hent den aktuelle brugers ID |
| POST | `/api/auth/logout` | Afslut HTTP-session |

Registrering kræver navn, gyldig e-mail og adgangskode. E-mail skal være unik (`409 Conflict` ved dublet), og adgangskoder hashes med bcrypt. Login returnerer blandt andet `token`, `tokenType` og `expiresIn`. Beskyttede endpoints kræver autentificering; systemet anvender JWT og HTTP-sessioner til blandt andet rejsekladder. Logout invaliderer sessionen, men tilbagekalder ikke nødvendigvis allerede udstedte JWT'er.

### Destinationssøgning og rejsekladde (4 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| GET | `/api/destinations?search=Oslo` | Søg destinationer i Danmark, Norge og Sverige |
| POST | `/api/trip-draft` | Vælg destination med f.eks. `{"destination":"OSL"}` (`201 Created`) |
| PUT | `/api/trip-draft/details` | Angiv afrejselufthavn, datoer, antal voksne og direkte fly |
| GET | `/api/trip-draft` | Hent den aktuelle rejsekladde |

Eksempel på `PUT /api/trip-draft/details`:

```json
{
  "origin": "CPH",
  "startDate": "2026-11-20",
  "endDate": "2026-11-25",
  "adults": 1,
  "directOnly": false
}
```

Datoer angives som `YYYY-MM-DD`. Antal voksne skal være et heltal fra 1 til 9, og slutdatoen skal ligge efter startdatoen.

### Destinationsinformation og oplevelser (4 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| GET | `/api/destinations/{airportCode}` | Hent destinationsinformation og inspiration |
| GET | `/api/experience-categories` | Hent interessekategorier |
| GET | `/api/destinations/{code}/experiences` | Hent og filtrer lokale oplevelser |
| GET | `/api/experiences/{id}` | Hent detaljer om én oplevelse |

Kategorierne er `mad`, `sevaerdigheder`, `shopping`, `natur`, `natteliv` og `familie`. Filteret understøtter kommaseparerede eller gentagne `categories`-parametre, f.eks. `GET /api/destinations/OSL/experiences?categories=natur,mad`. Flere kategorier kombineres med **ELLER**. Filtreringen ændrer ikke gemte rejser.

### Flysøgning og flyvalg (6 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| POST | `/api/flights/search` | Søg flytilbud til rejsekladden gennem Duffel |
| POST | `/api/trip-draft/flight` | Vælg et flytilbud fra seneste søgning med `offerId` |
| DELETE | `/api/trip-draft/flight` | Fjern valgt fly fra rejsekladden |
| POST | `/api/trips/{id}/flights/search` | Søg flytilbud til en gemt rejse |
| PUT | `/api/trips/{id}/flight` | Vælg eller udskift fly på gemt rejse med `offerId` |
| DELETE | `/api/trips/{id}/flight` | Fjern fly fra gemt rejse |

`POST /api/trip-draft/flight` **søger ikke** efter fly; det vælger et allerede fundet tilbud. Fjernelse af fly fra kladden bevarer destination, datoer og søgeresultater. Flytilbud er til planlægning, ikke faktiske bookinger.

### Hoteller og prisoversigt (7 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| POST | `/api/hotels/search` | Søg fiktive hoteltilbud til rejsekladden |
| POST | `/api/trip-draft/hotel` | Vælg eller udskift hotel med `hotelId` |
| DELETE | `/api/trip-draft/hotel` | Fjern hotel fra rejsekladden |
| GET | `/api/trip-draft/summary` | Hent prisoversigt for rejsekladden |
| POST | `/api/trips/{id}/hotels/search` | Søg hoteltilbud til en gemt rejse |
| PUT | `/api/trips/{id}/hotel` | Vælg eller udskift hotel på gemt rejse med `hotelId` |
| DELETE | `/api/trips/{id}/hotel` | Fjern hotel fra gemt rejse |

Hoteltilbud og hotelpriser er **fiktive testdata**, angivet i DKK. Rejsekladden skal have destination og gyldige datoer, før der kan søges. Hotelvalg i kladden ryddes ved ændring af destination eller rejseoplysninger samt efter gemning af rejsen. EUR-beløb omregnes med en **fast testkurs på 7,50 DKK pr. EUR**; det er ikke en aktuel valutakurs. En samlet pris kan være `null`, hvis en valuta ikke understøttes. Der foretages ingen reservation.

### Gemte rejser (3 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| POST | `/api/trips` | Gem rejse fra den aktuelle rejsekladde (`201 Created`) |
| GET | `/api/trips` | Hent brugerens gemte rejser |
| GET | `/api/trips/{id}` | Hent én gemt rejse |

Rejser er knyttet til den autentificerede bruger. Et ugyldigt rejse-ID kan give `400 Bad Request`; en rejse, der ikke findes eller ikke tilhører brugeren, kan give `404 Not Found`. Et rejseobjekt kan indeholde `id`, `createdAt`, `destination`, `search`, `flight`, `hotel` og `priceSummary`.

### Aktiviteter (5 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| POST | `/api/trips/{id}/activities` | Tilføj anbefalet oplevelse (`201 Created`) |
| POST | `/api/trips/{id}/activities/custom` | Opret egen aktivitet (`201 Created`) |
| GET | `/api/trips/{id}/activities` | Hent rejsens aktiviteter |
| PUT | `/api/trips/{id}/activities/{activityId}` | Rediger aktivitet |
| DELETE | `/api/trips/{id}/activities/{activityId}` | Slet aktivitet |

En anbefalet oplevelse oprettes med `experienceId`, `date` og `time`. En egen aktivitet kan angives med `name`, `date`, `time`, `priceAmount`, `category` og `notes`. Disse endpoints kræver autentificering og kontrollerer adgang til rejsen.

### Rejseplan og budget (3 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| GET | `/api/trips/{id}/itinerary` | Hent rejseplan med dage, fly, hotel, aktiviteter og budget |
| PUT | `/api/trips/{id}/budget` | Opret eller opdater budgetgrænse |
| DELETE | `/api/trips/{id}/budget` | Fjern budgetgrænse |

Eksempel på `PUT /api/trips/{id}/budget`:

```json
{"budgetAmount": 10000.00}
```

Budgetgrænsen skal være positiv, må højst være `9999999999.99` og må have maksimalt to decimaler. Budgettet viser kendte udgifter, eventuelle ukendte priser og resterende beløb, når det kan beregnes. Ikke-valgte fly og hoteller tæller som 0 DKK. Fly vises på afrejsedagen; aktivitetstider er lokale tider på destinationen.

### Vejr og anbefalinger (7 endpoints)

| Metode | Endpoint | Formål |
|---|---|---|
| GET | `/api/weather/airports` | Hent vejr for understøttede lufthavne |
| GET | `/api/weather/airports/{code}` | Hent vejr for én lufthavn |
| GET | `/api/weather/destinations/{code}` | Hent destinationsvejr og kommende dage |
| GET | `/api/weather/destinations/{code}/climate` | Hent historiske månedsgennemsnit |
| GET | `/api/weather/destinations/{code}/recommendations` | Hent vejrafhængige anbefalinger |
| GET | `/api/trip-draft/weather` | Hent vejr og anbefalinger til rejsekladden |
| GET | `/api/trips/{id}/weather` | Hent vejr og anbefalinger til gemt rejse |

Anbefalinger understøtter den valgfrie query-parameter `date=YYYY-MM-DD`. For rejsekladde og gemt rejse skal datoen ligge inden for rejsens periode; hvis den udelades, anvendes afrejsedatoen. Disse to rejserelaterede endpoints kræver autentificering. Historiske månedsgennemsnit er **ikke** en vejrprognose.

### HTTP-statuskoder

| Statuskode | Betydning |
|---|---|
| `200 OK` | Forespørgslen blev gennemført |
| `201 Created` | En ressource blev oprettet |
| `400 Bad Request` | Ugyldigt input, datoformat eller ID |
| `401 Unauthorized` | Manglende eller ugyldig autentificering |
| `404 Not Found` | Ressource eller adgangsberettiget rejse blev ikke fundet |
| `409 Conflict` | Konflikt med eksisterende data, f.eks. registreret e-mail |
| `502 Bad Gateway` | Problem med ekstern tjeneste eller ugyldige eksterne data |

De konkrete fejlbeskeder og statuskoder afhænger af endpointet og den underliggende service. Endpointlisten er gennemgået mod controllerkoden; den erstatter ikke en separat test af alle endpoints på den deployede server.

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