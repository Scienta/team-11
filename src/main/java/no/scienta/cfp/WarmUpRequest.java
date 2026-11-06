package no.scienta.cfp;

import java.util.List;

/**
 * One request that the service sends to itself at start-up, and the status it expects. Each request is rejected or
 * reads only, so the warm-up changes no data (workflow/specs/warm-up/spec.md, R1 and R2).
 */
record WarmUpRequest(String method, String path, String body, int expectedStatus) {

    static final List<WarmUpRequest> ALL = List.of(
            new WarmUpRequest("POST", "/speakers", "{}", 400),
            new WarmUpRequest("GET", "/speakers/0", "", 404),
            new WarmUpRequest("POST", "/talks", "{}", 400),
            new WarmUpRequest("GET", "/talks/0", "", 404),
            new WarmUpRequest("GET", "/talks?speakerId=0&sort=rating", "", 200),
            new WarmUpRequest("PUT", "/talks/0/ratings/1", "{\"score\":3}", 404));

    @Override
    public String toString() {
        return method + " " + path;
    }
}
