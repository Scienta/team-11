package no.scienta.cfp;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the warm-up when the service is ready: a background thread sends each warm-up request through the service's
 * own port, logs a warning for each unexpected outcome and one line at the end. The service takes traffic during the
 * warm-up, and /health does not change (workflow/specs/warm-up/spec.md).
 */
@Component
class StartupWarmUp {

    private static final Logger log = LoggerFactory.getLogger(StartupWarmUp.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    @EventListener(ApplicationReadyEvent.class)
    void warmUp(ApplicationReadyEvent event) {
        if (!(event.getApplicationContext() instanceof WebServerApplicationContext web)) {
            return;
        }
        int port = web.getWebServer().getPort();
        HttpWarmUpExchange exchange = new HttpWarmUpExchange(URI.create("http://localhost:" + port), REQUEST_TIMEOUT);
        Thread thread = new Thread(() -> run(exchange), "warm-up");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run(HttpWarmUpExchange exchange) {
        long started = System.nanoTime();
        List<WarmUpOutcome> outcomes = WarmUpRequest.ALL.stream()
                .map(exchange::send)
                .collect(Collectors.toUnmodifiableList());
        outcomes.forEach(outcome -> outcome.warning().ifPresent(log::warn));
        log.info("Warm-up done: {} requests in {} ms", outcomes.size(),
                Duration.ofNanos(System.nanoTime() - started).toMillis());
    }
}
