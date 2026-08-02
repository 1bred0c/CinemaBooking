# AI Movie Chat - Phase 2B Tool Calling

Phase 2B adds read-only access to current CinemaBooking data through Spring AI
tool calling. The model may request a tool call, but Java services validate and
execute every database operation.

## Available tools

- `searchShowtimes`: searches scheduled showtimes by movie title, optional
  cinema, date, and period.
- `searchShowtimesByMovieId`: searches by an exact movie UUID returned by hybrid
  retrieval, avoiding literal-title searches for plot or theme descriptions.
- `searchShowtimesByDate`: lists the movies and showtimes actually scheduled on
  a date when the user asks what is showing without naming a movie.
- `getShowtimeDetails`: returns verified details for one active showtime UUID.
- `getSeatAvailability`: returns point-in-time seat counts and price ranges by
  seat type.
- `getTicketPrices`: returns current show-seat prices grouped by seat type.

All tools are read-only. Booking creation, seat holding, cancellation, and
payment are intentionally unavailable.

## Routing

```text
Movie description/recommendation -> Phase 2A hybrid RAG
Live showtime/seat/price question -> Spring AI tools
Recommendation plus live question -> RAG context plus movie-ID tool
Generic "what is showing tomorrow?" -> date tool, without catalog-wide RAG
```

`ChatClient` manages the tool loop: it sends tool schemas, receives the model's
tool request, invokes the annotated Java method, returns the result to the
model, and receives the final natural-language answer.

Tool-enabled requests set OpenAI `reasoning_effort` to `none`. The current
Spring AI integration uses Chat Completions, where the configured GPT-5.6 model
rejects function tools combined with reasoning effort. Analyzer and reranker
requests do not expose tools and retain their normal model configuration.

## Data rules

- Only active movies, cinemas, rooms, and scheduled future showtimes are
  returned.
- Seat state comes from `ShowSeat`, never from the physical `Seat` entity.
- Prices come from show-seat prices and the showtime base price.
- A showtime UUID must originate from CinemaBooking tool results.
- Descriptive movie constraints are resolved by hybrid RAG before querying live
  data by movie UUID.
- Generic date-based discovery starts from scheduled showtimes, rather than
  recommending arbitrary catalog movies and checking them afterward.
- Availability is a snapshot and can change before a booking is confirmed.
- Tool-only responses have an empty movie `sources` list because they do not use
  vector documents.

## Configuration

```yaml
spring:
  ai:
    tools:
      throw-exception-on-error: ${AI_TOOL_THROW_ON_ERROR:true}
    chat:
      client:
        tool-calling:
          enabled: ${AI_TOOL_CALLING_ENABLED:true}
```

Tool execution errors are thrown to the application instead of being exposed to
the model as raw internal error messages.

## Example requests

```bash
curl -X POST "http://localhost:8080/api/v1/ai/chat" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Interstellar tối nay có suất nào?"
  }'
```

```bash
curl -X POST "http://localhost:8080/api/v1/ai/chat" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Gợi ý phim zombie và kiểm tra phim đó có suất tối nay không"
  }'
```

Follow-up references such as "suất thứ hai" require conversation memory and are
planned for the next phase. Until then, each request must contain enough context
for the model to select a movie or verified showtime.
