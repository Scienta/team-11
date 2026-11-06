package no.scienta.cfp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends one warm-up request to the service over HTTP and turns the result into an outcome. It never throws, so one
 * failed request cannot stop the warm-up (workflow/specs/warm-up/spec.md, R4).
 */
final class HttpWarmUpExchange {

    private final URI service;
    private final Duration timeout;
    private final HttpClient http;

    HttpWarmUpExchange(URI service, Duration timeout) {
        this.service = service;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    WarmUpOutcome send(WarmUpRequest request) {
        try {
            HttpResponse<Void> response = http.send(toHttp(request), HttpResponse.BodyHandlers.discarding());
            return new WarmUpOutcome.Answered(request, response.statusCode());
        } catch (IOException e) {
            return new WarmUpOutcome.Failed(request, e.getClass().getSimpleName() + " " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new WarmUpOutcome.Failed(request, "interrupted");
        }
    }

    private HttpRequest toHttp(WarmUpRequest request) {
        HttpRequest.BodyPublisher body = request.body().isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(request.body());
        return HttpRequest.newBuilder(service.resolve(request.path()))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .method(request.method(), body)
                .build();
    }
}
