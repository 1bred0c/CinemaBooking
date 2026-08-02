# AI Movie Chat - Phase 2A

Phase 2A upgrades retrieval from vector-only search to a hybrid pipeline. It does
not add tool calling or live showtime/booking actions.

## Request flow

1. `ChatQueryAnalyzer` asks the chat model for a validated `ChatQueryPlan`.
2. Non-search intents such as greeting, help, live-data and out-of-scope requests are routed directly.
3. `HybridMovieRetriever` runs two retrieval channels:
   - semantic similarity through the existing PGVector store;
   - PostgreSQL full-text/exact-field search over movie title, description, director and genres.
4. Relational metadata filters are applied for genre, director, maximum duration,
   release date and viewer age. Genre matching uses OR semantics; candidates
   matching more requested genres are ranked first.
5. Reciprocal Rank Fusion combines vector and keyword ranks without assuming
   their scores share the same scale.
6. `MovieReranker` asks the model to reorder only the retrieved candidate IDs.
   Invalid IDs or model failures fall back to RRF order.
7. The final answer model receives only the final ranked movies. API `sources` are built from the same list.

## Configuration

```yaml
app:
  ai:
    rag:
      top-k: ${AI_RAG_TOP_K:3}
      candidate-k: ${AI_RAG_CANDIDATE_K:10}
      rrf-constant: ${AI_RAG_RRF_CONSTANT:60}
      rerank-enabled: ${AI_RAG_RERANK_ENABLED:true}
      similarity-threshold: ${AI_RAG_SIMILARITY_THRESHOLD:0.00}
```

`candidate-k` must be greater than or equal to `top-k`. Disable reranking when
testing latency/cost with `AI_RAG_RERANK_ENABLED=false`.

## Operational notes

- No new PostgreSQL extension or migration is required; keyword retrieval uses
  built-in PostgreSQL full-text search.
- Existing Phase 1 embeddings are reused, so a reindex is unnecessary unless
  movie data changed or vector documents are missing.
- A normal movie-search request can use three chat-model calls (analysis, reranking, answer) plus one embedding call.
- Query-analysis and reranking failures degrade safely to raw-query search and RRF order.

## Live Neon smoke test

The database smoke test is opt-in so the normal test suite does not depend on
credentials or network access:

```powershell
$env:RUN_AI_HYBRID_LIVE_TEST='true'
mvn -Dtest=MovieKeywordSearchSmokeTest test
```

It expects an active `The Martian` movie and at least one active `Action` movie
in the configured database.
