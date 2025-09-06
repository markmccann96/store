package com.example.store.utils;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

public class ResponseUtility {

    private ResponseUtility() {
        // this class is a static utility. Should not be instantiated
    }

    /** Builds RFC 8288 Link header using current request URL and query params. */
    public static String buildLinkHeader( long limit, int offset, long total) {
        UriComponentsBuilder base = ServletUriComponentsBuilder.fromCurrentRequest();
        List<String> links = new ArrayList<>();

        // first
        String first = base.replaceQueryParam("limit", limit)
                .replaceQueryParam("offset", 0)
                .toUriString();
        links.add("<" + first + ">; rel=\"first\"");

        // last
        if (total > 0) {
            long lastOffset = ((total - 1) / limit) *  limit;
            String last = base.replaceQueryParam("limit", limit)
                    .replaceQueryParam("offset", lastOffset)
                    .toUriString();
            links.add("<" + last + ">; rel=\"last\"");
        }

        // prev
        if (offset > 0) {
            long prevOffset = Math.max(offset - limit, 0L);
            String prev = base.replaceQueryParam("limit", limit)
                    .replaceQueryParam("offset", prevOffset)
                    .toUriString();
            links.add("<" + prev + ">; rel=\"prev\"");
        }

        // next
        if (offset + limit < total) {
            long nextOffset = offset + limit;
            String next = base.replaceQueryParam("limit", limit)
                    .replaceQueryParam("offset", nextOffset)
                    .toUriString();
            links.add("<" + next + ">; rel=\"next\"");
        }

        return String.join(", ", links);
    }
}
