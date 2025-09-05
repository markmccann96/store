# ADR0002: API Pagination Strategy (limit/offset + Link & X-Total-Count)

## Status

Accepted

## Context

Our API already exposes collection endpoints (e.g., `/customer`, `/order`) and a new search endpoint (`/customer/search`). As data volume grows, returning full result sets is inefficient and degrades latency and memory usage for both server and clients. We need a consistent, documented pagination approach that:

* Is simple to consume from browsers, mobile, and server clients.
* Works with our existing data stores and ORM/repositories.
* Plays well with HTTP caching and CORS.
* Is easy to express and validate in OpenAPI and auto-generated SDKs.

Constraints / assumptions:

* We require deterministic ordering for repeatable pagination.
* We prefer a minimal change surface to existing endpoints.
* We may later introduce cursor-based pagination where offset is too costly at scale.
* We want clients to discover next/prev pages from headers without coupling to response body shapes.

## Decision

Adopt **limit/offset query parameters** with pagination **metadata in HTTP headers** for all collection/search endpoints.

**Parameters**

* `limit` (optional, int, default **25**, max **200**): maximum number of items to return.
* `offset` (optional, int, default **0**, min **0**): number of items to skip (0-based).

**Headers**

* `X-Total-Count`: total number of matching records **before** pagination (integer).
* `Link`: pagination navigation per RFC 8288 (formerly 5988). May include `rel="next"`, `prev`, `first`, and `last`.
  Example:
  `<http://localhost:8080/customer/search?name=ann&limit=25&offset=25>; rel="next", <http://localhost:8080/customer/search?name=ann&limit=25&offset=0>; rel="first"`

**CORS**

* Expose headers to browsers: `Access-Control-Expose-Headers: X-Total-Count, Link`.

**OpenAPI**

* Document `limit`/`offset` as optional query params on all list/search endpoints.
* Document `X-Total-Count` and `Link` under `responses.headers` for 200 responses.

**Validation & Errors**

* Requests exceeding max `limit` or with negative `offset` return `400 Bad Request` with a clear error body.
* If totals are expensive to compute, allow an **implementation detail** to use an efficient count strategy; if approximation is ever used, we will document it explicitly in that endpoint (not adopted now).

**Ordering**

* Each paginated endpoint must define a **stable default sort** (e.g., `createdAt ASC`, `name ASC`). If clients can request sort order in the future, those params must be included in the `Link` URLs to keep navigation consistent.

**Applicability**

* Apply to: `/customer`, `/customer/search`, `/order`, `/products` (and future collection endpoints).

## Consequences

**Positive**

* Simple, widely understood pagination model; easy for Swagger-generated SDKs and REST clients.
* Header-based navigation (`Link`) keeps response bodies clean and consistent across endpoints.
* `X-Total-Count` enables UI page controls (page numbers, progress indicators).
* Minimal changes to existing controllers/repositories; straightforward SQL/ORM support.

**Negative / Trade-offs**

* **High offset cost**: `OFFSET n` can degrade as `n` grows on large tables; may require indexes and query tuning.
* **Shifting data window**: inserts/deletes between requests can cause duplicates/misses across pages without cursoring or snapshotting.
* **Count cost**: `X-Total-Count` can be expensive on complex filters; may require cached/estimated counts or separate count queries.

**Future Impact**

* We can introduce **cursor-based pagination** for very large datasets or deep scrolling while preserving this ADR for most endpoints.
* We may add optional query params later (`sort`, `order`, `fields`)—`Link` headers will propagate them.
* Observability: add metrics on pagination usage (typical `limit`, average `offset`, count timings) to guide performance work.
* DB/Indexing: ensure appropriate indexes exist for each endpoint’s default sort/filter to keep `limit/offset` performant.
