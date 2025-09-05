# ADR0003: Speed up `GET /orders` Fetch All performance

## Status

Proposed

## Context

The `GET /orders` endpoint supports optional `limit`/`offset`. For backward compatibility, when `limit=null` we return **all orders**. This “fetch-all” path is slow; the dominant cost is database latency and transferring a large result (plus ORM materialization), not application CPU.

Constraints / assumptions:

* API response shape must remain unchanged (still returns the full collection when `limit=null`).
* Clients may repeatedly poll this endpoint.
* We can add DB indexes, read-only transaction hints, and HTTP caching headers.
* We can rely on an `updated_at` column that is reliably maintained on inserts/updates/deletes (or equivalent change watermark).
* Tech: Postgres; Spring Boot + Spring Data JPA/JDBC.
* **We will not change the controller method signature or introduce streaming at this time.**

## Decision

Implement optimizations for the `limit=null` path **without streaming**:

1. **Projection-based query (no entity graph materialization)**

    * Replace `findAll()` + mapping with a **narrow projection** (JPA projection or JDBC) selecting only fields required by the wire format.
    * Use a **deterministic order** (`created_at DESC, id DESC`) and ensure a supporting index exists.

2. **Conditional GET validators (ETag / Last-Modified)**

    * Compute a cheap “snapshot tag” from the table, e.g. `ETag = SHA-256(count(*) | max(updated_at))`, and `Last-Modified = max(updated_at)`.
    * On requests with `If-None-Match` / `If-Modified-Since`, return **304 Not Modified** with no body when unchanged.
    * Expose `ETag` / `Last-Modified` to browsers via `Access-Control-Expose-Headers` as needed.

3. **HTTP compression**

    * Enable GZIP/deflate for large JSON responses to reduce transfer time and bandwidth.

Non-goals (explicitly not doing now):

* Whole-payload Redis caching for the fetch-all result.
* Changing API semantics (still returns all orders when `limit=null`).
* **Streaming responses** (kept as an alternative for later).
* Replacing offset pagination generally (page/cursor changes may be proposed separately).

Implementation notes (high level):

* Add indexes: `(created_at DESC, id DESC)` for ordering; `(updated_at)` for the snapshot query.
* JPA: create an `OrderRow` projection interface and a `findAllRows()` method with read-only/fetch size hints; map rows to `OrderDTO`.
* Controller: branch on `limit==null` → compute/validate ETag → if matched, return 304; else run projection query, map to DTO list, return 200 with validators.
* Properties: enable `server.compression`; set `hibernate.jdbc.fetch_size`; expose relevant headers for CORS front-ends.

## Consequences

**Positive outcomes**

* **Faster repeat polls:** Clients honoring validators receive **304 Not Modified** (minimal latency and bandwidth).
* **Lower DB/app work on 200 responses:** Projections avoid loading full entity graphs; fetch size helps transfer and memory behavior.
* **Smaller payloads on the wire:** Compression reduces response size significantly.
* **Backward compatible & low risk:** No change to method signature or media type.

**Negative outcomes / risks / trade-offs**

* **200-case still builds the full list:** On actual changes (or first call), we still materialize all rows and serialize the array.
* **Validator correctness depends on `updated_at`:** Must update on all mutations; otherwise false 304s are possible. Mitigate with DB trigger or strict app policy.
* **Small overhead for snapshot query:** `COUNT(*)` + `MAX(updated_at)` adds minimal load; keep indexes to ensure it’s cheap.
* **Clients must send validators** to benefit; without them, it’s always a 200.

**How this impacts future decisions or constraints**

* Establishes a pattern: **projections + conditional GET + compression** before considering distributed caches.
* Leaves room for further improvements if allowed to adjust workflow:

    * **Alternative (future): Streaming**

        * Add a **new endpoint** (e.g., `/orders/stream`) using **NDJSON** with `StreamingResponseBody`, or
        * Regenerate with **WebFlux** and return `Flux<OrderDTO>` (stream JSON array) while preserving the main route.
          These options reduce memory and time-to-first-byte but require client and/or codegen workflow changes.
    * Route this endpoint to a **read replica** to offload the primary.
    * Use a **materialized view/denormalized read model** to accelerate the full snapshot.
    * Encourage clients toward **pagination or cursor-based APIs** for very large datasets.
