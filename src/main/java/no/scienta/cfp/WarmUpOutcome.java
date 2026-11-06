package no.scienta.cfp;

import java.util.Optional;

/**
 * What happened to one warm-up request: the service answered it, or it failed. Only an unexpected status or a failure
 * gives a warning (workflow/specs/warm-up/spec.md, R4).
 */
sealed interface WarmUpOutcome {

    Optional<String> warning();

    /** The service answered with a status, expected or not. */
    record Answered(WarmUpRequest request, int status) implements WarmUpOutcome {
        @Override
        public Optional<String> warning() {
            return status == request.expectedStatus()
                    ? Optional.empty()
                    : Optional.of("Warm-up " + request + " answered " + status + ", expected " + request.expectedStatus());
        }
    }

    /** The request got no answer: no connection, a time-out, or an interrupt. */
    record Failed(WarmUpRequest request, String reason) implements WarmUpOutcome {
        @Override
        public Optional<String> warning() {
            return Optional.of("Warm-up " + request + " failed: " + reason);
        }
    }
}
