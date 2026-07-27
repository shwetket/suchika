package com.suchika.gateway.config;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

/**
 * Ensures that deep links (like /dashboard or /health/vitals) in the bundled 
 * React Single Page Application (SPA) return the index.html file instead of a 404,
 * while allowing API calls (/v1/*) and static assets (*.js, *.css) to pass through.
 */
public class SpaFilter {

    @RouteFilter(400) 
    void spaFallback(RoutingContext rc) {
        String path = rc.request().path();
        
        // Skip API routes and Quarkus internal routes
        if (path.startsWith("/v1/") || path.startsWith("/q/")) {
            rc.next();
            return;
        }
        
        // If it's a GET request and doesn't look like a file request (no dot in the last segment), 
        // reroute to the root (which serves index.html)
        if (rc.request().method().name().equals("GET") && !path.substring(path.lastIndexOf('/') + 1).contains(".")) {
            rc.reroute("/");
        } else {
            rc.next();
        }
    }
}
