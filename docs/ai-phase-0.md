# AI Chat Phase 0

Phase 0 proves the smallest useful Spring AI flow:

```text
HTTP request -> AiChatController -> AiChatService -> ChatClient
             -> OpenAI ChatModel -> OpenAI API
```

It deliberately has no RAG, vector database, tools, streaming, or chat
memory. The assistant therefore must not claim that it knows current movies,
showtimes, seats, or prices from CinemaBooking.

## Spring AI concepts used

- `ChatModel` is the provider-neutral interface implemented by the OpenAI
  starter.
- `ChatClient.Builder` is auto-configured by Spring AI from that model.
- `ChatClient` is the fluent API used to create the system and user messages,
  invoke the provider, and read the response content.
- The OpenAI starter reads its API key and model from Spring configuration.

Application code depends on `ChatClient`, not on an OpenAI-specific Java
client. A later provider change therefore affects configuration and adapters,
not the controller contract.

## Enable locally

Add these values to the local `.env` file. Never commit the real API key.

```properties
AI_CHAT_PROVIDER=openai
OPENAI_API_KEY=your-rotated-key
OPENAI_CHAT_MODEL=gpt-5.6-luna
```

OpenAI is the default chat provider for Phase 0, so `AI_CHAT_PROVIDER` and
`OPENAI_CHAT_MODEL` may be omitted unless you want to override them. A valid
`OPENAI_API_KEY` is required when the application starts. The application
loads this value directly from `.env` before Spring Boot creates its context
and writes it to the canonical `spring.ai.openai.api-key` system property.
This intentionally prevents a stale `OPENAI_API_KEY` process environment
variable from overriding the project file. The application fails fast if the
file or entry is missing.

## API

The endpoint requires the same JWT authentication as other protected APIs.

```http
POST /api/v1/ai/chat
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "message": "Gợi ý một bộ phim khoa học viễn tưởng nổi tiếng"
}
```

Example response:

```json
{
  "message": "..."
}
```

The request is rejected when `message` is blank or longer than 2,000
characters. Provider errors are mapped to HTTP 503 by the existing centralized
exception handler.

## Tests

`AiChatServiceImplTest` mocks `ChatClient`, so normal test runs never spend API
credit.

`AiChatLiveTest` is opt-in and makes one real provider request:

```powershell
$env:RUN_OPENAI_LIVE_TEST="true"
./mvnw -Dtest=AiChatLiveTest test
```

The live test also requires `OPENAI_API_KEY` in the process environment. It is
not enabled during the normal Maven test suite.
