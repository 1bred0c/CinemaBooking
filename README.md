# CinemaBooking API

A REST API for cinema management and the booking flow from temporary seat holds
through VNPay or MoMo payment confirmation. The project demonstrates a layered
Spring Boot architecture, JWT authentication, validation, soft deletion,
transaction boundaries, idempotency, concurrency control, event-driven
notifications, and a grounded AI movie assistant.

## Technology

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA and Hibernate
- Spring Security with JWT
- PostgreSQL with pgvector
- Spring AI and OpenAI
- Apache Kafka and the transactional outbox pattern
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

The AI subsystem follows the same layering while separating model reasoning
from trusted application data:

```text
AI Chat Controller
  -> Query Analyzer
    -> Hybrid Movie Retrieval (vector + keyword + RRF)
    -> LLM Reranking
    -> Read-only Cinema Tools
  -> Conversation Memory Service
    -> Conversation and Message Repositories
```

## Available modules

- Authentication: registration, login, refresh token, and logout
- Cinema CRUD
- Room CRUD
- Physical Seat CRUD
- Movie CRUD
- Genre CRUD and Movie-Genre assignment
- Showtime CRUD and filtering
- Showtime seat generation and pricing
- Temporary seat holds with concurrency-safe creation and cancellation
- Booking creation and cancellation
- Idempotent payment attempts using VNPay or MoMo
- Signed return/IPN callback processing
- Kafka notifications backed by a transactional outbox
- Hybrid RAG movie discovery using vector and keyword search
- Read-only AI tool calling for showtimes, seats, and ticket prices
- Persistent per-user AI conversations with bounded context and rolling summaries

Administrative write endpoints require the `ADMIN` role. Read endpoints require
an authenticated user.

## Conceptual domain model

The ERD is maintained as Mermaid source so it stays reviewable alongside code.
It is conceptual and intentionally omits most columns and enum values.

```mermaid
erDiagram
    USER ||--o{ REFRESH_TOKEN : owns
    USER ||--o{ BOOKING : creates
    USER ||--o{ SHOW_SEAT_HOLD : creates
    USER ||--o{ NOTIFICATION : receives
    USER ||--o| AI_CONVERSATION : owns

    CINEMA ||--o{ ROOM : contains
    ROOM ||--o{ SEAT : contains
    ROOM ||--o{ SHOWTIME : hosts

    MOVIE ||--o{ SHOWTIME : schedules
    MOVIE ||--o{ MOVIE_GENRE : categorized_by
    GENRE ||--o{ MOVIE_GENRE : classifies

    SHOWTIME ||--o{ SHOW_SEAT : materializes
    SEAT ||--o{ SHOW_SEAT : becomes

    SHOWTIME ||--o{ SHOW_SEAT_HOLD : reserved_for
    SHOW_SEAT_HOLD ||--o{ SHOW_SEAT_HOLD_ITEM : contains
    SHOW_SEAT ||--o{ SHOW_SEAT_HOLD_ITEM : references

    SHOWTIME ||--o{ BOOKING : booked_for
    SHOW_SEAT_HOLD ||--o| BOOKING : confirms
    BOOKING ||--o{ BOOKING_ITEM : contains
    SHOW_SEAT ||--o{ BOOKING_ITEM : selects
    BOOKING ||--o{ PAYMENT : attempts

    AI_CONVERSATION ||--o{ AI_CHAT_MESSAGE : stores

    USER {
        uuid id PK
        string phone_number UK
        string role
        boolean is_active
    }
    MOVIE {
        uuid id PK
        string title
        boolean is_active
    }
    SHOWTIME {
        uuid id PK
        uuid movie_id FK
        uuid room_id FK
        datetime start_time
        string status
    }
    SHOW_SEAT {
        uuid id PK
        uuid showtime_id FK
        uuid seat_id FK
        decimal price
        string status
    }
    BOOKING {
        uuid id PK
        uuid user_id FK
        uuid showtime_id FK
        uuid hold_id FK
        string status
        decimal total_amount
    }
    PAYMENT {
        uuid id PK
        uuid booking_id FK
        string provider
        string status
        decimal amount
    }
    AI_CONVERSATION {
        uuid id PK
        uuid user_id FK
        text rolling_summary
        long completed_turns
        long summarized_turns
    }
    AI_CHAT_MESSAGE {
        uuid id PK
        uuid conversation_id FK
        long sequence_number
        string role
        text content
    }
```

Key concepts:

- A Cinema contains Rooms, and each Room contains its physical Seats.
- Movies and Rooms are connected through Showtimes.
- Movies can belong to multiple Genres.
- A ShowSeat represents one physical Seat for one Showtime, with
  screening-specific availability and pricing.
- A ShowSeatHold temporarily reserves ShowSeats for a User through
  ShowSeatHoldItems.
- A Booking belongs to a User and a Showtime, while BookingItems identify its
  selected ShowSeats.
- A Booking can have multiple Payment attempts so a failed attempt can be
  retried.
- Each User owns at most one AI conversation. The full message history is
  persisted, but it is not sent wholesale to the model.

## AI assistant workflow

The analyzer returns a structured intent and search plan. Application code then
selects the appropriate grounded path instead of allowing the model to query the
database directly.

```mermaid
flowchart TD
    A[Authenticated user message] --> B[Load rolling summary and five recent turns]
    B --> C[LLM query analyzer]
    C --> D{Intent}

    D -->|Movie discovery or information| E[Hybrid movie retrieval]
    D -->|Specific live-data request| J[Spring AI tool calling]
    D -->|Descriptive movie plus live data| E
    D -->|Greeting, help, or out of scope| N[Direct bounded response]

    E --> F[Vector search in pgvector]
    E --> G[PostgreSQL keyword search]
    F --> H[RRF candidate fusion and metadata filters]
    G --> H
    H --> I[LLM reranking constrained to retrieved movie IDs]

    I --> K{Live data required?}
    K -->|No| L[Grounded final answer]
    K -->|Yes| J

    J --> M[Read-only Java tools]
    M --> M1[Showtimes by title, movie ID, or date]
    M --> M2[Showtime details and seat availability]
    M --> M3[Current ticket prices]
    M1 --> L
    M2 --> L
    M3 --> L
    N --> O[Persist completed user and assistant turn]
    L --> O
    O --> P{Five new completed turns?}
    P -->|No| Q[Return response]
    P -->|Yes| R[Update rolling summary]
    R --> Q
```

### Memory boundaries

- The database stores the complete conversation for history and UI reload.
- Model calls receive only the rolling summary, the five latest completed
  turns, and the current message.
- Every five newly completed turns, the previous summary and only the
  unsummarized turns produce a new rolling summary.
- Historical showtimes, prices, and seat counts are considered stale and must
  be verified again through tools.
- Tool-call protocol messages are not persisted; only the final assistant
  response is stored.

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
OPENAI_API_KEY=your-openai-api-key
OPENAI_CHAT_MODEL=your-supported-chat-model
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
SEAT_HOLD_DURATION_MINUTES=10
VNPAY_TMN_CODE=your-vnpay-terminal-code
VNPAY_HASH_SECRET=your-vnpay-hash-secret
VNPAY_RETURN_URL=http://localhost:8080/api/v1/payments/vnpay/return
VNPAY_IPN_URL=https://your-public-domain.example/api/v1/payments/vnpay/ipn
MOMO_PARTNER_CODE=your-momo-partner-code
MOMO_ACCESS_KEY=your-momo-access-key
MOMO_SECRET_KEY=your-momo-secret-key
MOMO_REDIRECT_URL=http://localhost:8080/api/v1/payments/momo/return
MOMO_IPN_URL=https://your-public-domain.example/api/v1/payments/momo/ipn
```

The local `.env` file is ignored by Git and loaded automatically by Spring Boot.
Provider IPN endpoints require a public HTTPS URL when testing with a sandbox.

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

The test suite includes application-context and focused Movie and Payment
service tests. Provider sandbox calls require valid merchant credentials and are
not executed by the unit test suite.

## Domain notes

- Deletes are soft deletes when a domain object has an active state.
- A physical Seat does not store screening-specific availability.
- Movie and Genre use a many-to-many join table named `movie_genre`.
- Movie and Room are connected through Showtime.
- Showtime scheduling locks the Room row before checking for overlapping
  screenings, preventing concurrent creation for the same room from passing the
  conflict check simultaneously.
- Expired holds and incomplete reservations are processed by scheduled workers.
- Payment initialization calls run outside database transactions; short
  transactions are used before and after provider network I/O.
- Signed provider callbacks atomically confirm the Booking, succeed the Payment,
  and change held ShowSeats to booked.
