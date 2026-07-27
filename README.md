# CinemaBooking API

A REST API for managing cinemas, rooms, physical seats, movies, genres, and
showtimes. The project demonstrates a layered Spring Boot architecture, JWT
authentication, role-based authorization, validation, soft deletion, and
concurrency-aware showtime scheduling.

## Technology

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA and Hibernate
- Spring Security with JWT
- PostgreSQL
- Maven
- Swagger/OpenAPI

## Architecture

```text
Controller
  -> Service interface
    -> Service implementation
      -> Repository
        -> Entity
```

API input and output use DTOs; controllers do not return JPA entities.

## Available modules

- Authentication: registration, login, refresh token, and logout
- Cinema CRUD
- Room CRUD
- Physical Seat CRUD
- Movie CRUD
- Genre CRUD and Movie–Genre assignment
- Showtime CRUD and filtering

Administrative write endpoints require the `ADMIN` role. Read endpoints require
an authenticated user.

## Local setup

### Prerequisites

- JDK 17
- PostgreSQL

### Environment

Copy `.env.example` to `.env` and provide local values:

```properties
DB_URL=jdbc:postgresql://localhost:5432/cinema_booking
DB_USERNAME=postgres
DB_PASSWORD=your-password
JWT_SECRET=your-random-secret-at-least-32-characters-long
```

The local `.env` file is ignored by Git and loaded automatically by Spring Boot.

### Run

On Windows:

```shell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```shell
./mvnw spring-boot:run
```

## API documentation

After starting the application:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Use the authentication login endpoint to obtain an access token. In Swagger UI,
select **Authorize** and enter the JWT; Swagger sends it as a Bearer token.

## Tests

```shell
./mvnw test
```

The test suite currently includes application-context and focused Movie service
tests.

## Domain notes

- Deletes are soft deletes when a domain object has an active state.
- A physical Seat does not store screening-specific availability.
- Movie and Genre use a many-to-many join table named `movie_genre`.
- Movie and Room are connected through ShowTime.
- Showtime scheduling locks the Room row before checking for overlapping
  screenings, preventing concurrent creation for the same room from passing the
  conflict check simultaneously.
