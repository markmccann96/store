package com.example.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapshotTagService {
    private final JdbcTemplate jdbcTemplate;

    public record Snapshot(String etag, long lastModified) {}

    // this generates a quick validation to see if the orders table has changed since the last query.
    // we use the count of the number of orders and the last updated time in the table to check if anything
    // has changed on this table. This will catch CREATE, UPDATE or DELETE operations on the table with low overhead
    // since there is an index on the order updated_at column.
    public Snapshot current() {
        var row = jdbcTemplate.queryForMap("select coalesce(max(updated_at), 'epoch') as maxu, count(*) as cnt from order");
        java.time.Instant maxUpdated = (row.get("maxu") instanceof java.sql.Timestamp ts)
                ? ts.toInstant()
                : java.time.Instant.EPOCH;
        long count = ((Number) row.get("cnt")).longValue();

        String basis = count + "|" + maxUpdated.toEpochMilli();
        String etag = "\"" + sha256Base64Url(basis) + "\""; // quotes are required per RFC
        return new Snapshot(etag, maxUpdated.toEpochMilli());
    }

    // This function checks if the snapshot etag is still valid.
    public boolean matchesConditional(String ifNoneMatch, long ifModifiedSince, Snapshot snap) {
        boolean etagMatch = ifNoneMatch != null && ifNoneMatch.equals(snap.etag());
        boolean timeMatch = (ifModifiedSince > 0) && (snap.lastModified <= ifModifiedSince);
        // If client sent both, HTTP says both must match to return 304
        if (ifNoneMatch != null && ifModifiedSince > 0) return etagMatch && timeMatch;
        if (ifNoneMatch != null) return etagMatch;
        if (ifModifiedSince > 0) return timeMatch;
        return false;
    }

    // this is used to generate a unique value for the etag for a given count & last updated at date.
    private static String sha256Base64Url(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
