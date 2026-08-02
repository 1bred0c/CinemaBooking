# AI Phase 1: Movie RAG with Spring AI and Neon PGVector

## Source of truth

`Movie` and its `genres` relationship are the source of truth. Admins keep using
the existing Movie APIs. They do not create a second AI-specific movie record.

`MovieDocumentFactory` projects these fields into a Spring AI `Document`:

- title
- description
- genres
- director
- duration
- release date
- age rating

The document metadata contains `movieId`, `title`, `active`, `sourceType`,
`genres`, and `ageRating`. Poster and trailer URLs are deliberately not embedded
because they do not improve semantic retrieval.

## Neon setup

Run this once in the Neon SQL Editor:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

The application uses the existing JDBC datasource for both JPA and Spring AI's
PGVector store. On startup, Spring AI initializes `movie_vector_store` when
`AI_VECTOR_INITIALIZE_SCHEMA=true`.

Optional `.env` overrides:

```properties
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
OPENAI_EMBEDDING_DIMENSIONS=1536
AI_RAG_TOP_K=5
AI_RAG_SIMILARITY_THRESHOLD=0.0
AI_VECTOR_INITIALIZE_SCHEMA=true
```

The embedding model and dimensions are a pair. Changing either requires a full
reindex and may require recreating the vector table.

The Phase 1 threshold defaults to `0.0` so retrieval scores can be calibrated
against real Movie data without accidentally dropping relevant candidates. Set
a higher value only after evaluating representative user questions; similarity
scores are model- and dataset-specific, not universal percentages.

## Index synchronization

Create, update, genre changes, and deactivate operations publish a
`MovieIndexRequested` event inside the Movie transaction. The
`MovieIndexEventListener` runs only after that transaction commits:

- active movie: replace its vector document
- inactive movie: remove its vector document
- failure: log the movie ID and preserve the committed Movie data

This avoids calling OpenAI before the business transaction is known to be valid.
The admin reindex endpoints repair missed synchronization:

```http
POST /api/v1/admin/ai/index/movies/{movieId}
POST /api/v1/admin/ai/index/movies/reindex-all
```

Both endpoints require an ADMIN JWT. A full reindex removes stale Movie
documents, creates embeddings in a batch, and adds all active movies again.

## Query flow

`POST /api/v1/ai/chat` performs this sequence:

1. `MovieKnowledgeRetriever` embeds the user's message.
2. PGVector returns up to `top-k` similar active Movie documents above the
   configured similarity threshold.
3. If there are no matches, the service returns a deterministic no-result answer
   without spending a chat-model call.
4. Retrieved documents are placed in a clearly delimited context section.
5. `ChatClient` answers only from that context.
6. The API returns the answer plus Movie source IDs, titles, and similarity
   scores so the frontend can render links or movie cards.

Example response:

```json
{
  "message": "Bạn có thể thử Interstellar...",
  "sources": [
    {
      "movieId": "00000000-0000-0000-0000-000000000000",
      "title": "Interstellar",
      "score": 0.91
    }
  ]
}
```

Phase 1 deliberately does not retrieve live showtimes, seats, prices, bookings,
or payments. Those require tool calling or deterministic service queries in a
later phase.
