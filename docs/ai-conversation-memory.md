# AI Conversation Memory

CinemaBooking stores one AI conversation per user and keeps its complete message
history for UI reload and audit. The complete history is never sent to the
model.

## Prompt strategy

Each request receives only:

- the latest rolling summary;
- the five most recent completed turns (ten user/assistant messages at most);
- the current user message;
- RAG context or live tool results required for the current intent.

After every five newly completed turns, a separate model call updates one
rolling summary from the previous summary plus only the unsummarized turns.
Recent turns intentionally overlap the summary to preserve precise follow-up
references.

Old showtimes, prices, and seat counts in memory are explicitly treated as
stale and must be verified again with CinemaBooking tools.

## Persistence

- `ai_conversations`: one row per user, rolling summary and sequence counters.
- `ai_chat_messages`: complete ordered USER and ASSISTANT message history.

Tool-call protocol messages are not persisted. The final assistant response is
persisted, while live IDs and data continue to come from verified tool results.

## Configuration

```yaml
app:
  ai:
    memory:
      recent-turns: ${AI_MEMORY_RECENT_TURNS:5}
      summary-interval-turns: ${AI_MEMORY_SUMMARY_INTERVAL_TURNS:5}
```

## API

`GET /api/v1/ai/chat/history` returns the authenticated user's complete saved
conversation as DTOs. `POST /api/v1/ai/chat` records a turn only after a final
assistant response has been produced successfully.
