# CinemaBooking Project Guidelines

## Project
- Java 17
- Spring Boot
- Spring Data JPA
- Spring Security
- PostgreSQL
- Maven
- Lombok
- Jakarta Validation

## Architecture
Follow this dependency direction:

Controller
-> Service interface
-> Service implementation
-> Repository
-> Entity

Use DTOs for API input and output.
Do not return JPA entities directly from controllers.

## Coding conventions
- Use constructor injection with @RequiredArgsConstructor.
- Use records for request and response DTOs when suitable.
- Use UUID primary keys.
- Use FetchType.LAZY for @ManyToOne relationships.
- Use @Transactional(readOnly = true) at service class level.
- Add @Transactional to write methods.
- Use Java Time API instead of java.sql.Timestamp.
- Use BigDecimal for money.
- Use EnumType.STRING for enums.
- Do not use field injection.
- Do not add bidirectional JPA relationships unless required.
- Do not use repository.save() after modifying an entity already loaded
  inside a transaction unless necessary.

## API conventions
- Base path: /api/v1
- POST returns 201 Created.
- DELETE means soft delete/deactivate when the entity has an active field.
- PATCH supports partial updates.
- Validation errors and domain exceptions are handled centrally.

## Domain rules
- Cinema has many rooms.
- Room has many physical seats.
- Movie and Room are connected through Showtime.
- ShowSeat represents one Seat in one Showtime.
- Booking belongs to one User and one Showtime.
- Booking contains BookingSeat records.
- Payment belongs to Booking.
- Seat state for a particular screening must not be stored in Seat.

## Scope control
- Do not redesign unrelated modules.
- Do not rename public API fields unless explicitly requested.
- Do not modify database migrations unrelated to the task.
- Before coding, inspect existing patterns in similar modules.
- After coding, run tests or at least Maven compilation.
- Clearly report changed files and any assumptions.