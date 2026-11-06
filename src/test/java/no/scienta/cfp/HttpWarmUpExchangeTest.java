package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

import com.sun.net.httpserver.HttpServer;

/**
 * How the warm-up turns one HTTP exchange into an outcome, so that a request without an answer cannot stop the warm-up
 * (workflow/specs/warm-up/spec.md, R4).
 */
@DisplayName("Warm-up HTTP exchange")
public class HttpWarmUpExchangeTest {

    private static final WarmUpRequest UNKNOWN_SPEAKER = new WarmUpRequest("GET", "/speakers/0", "", 404);

    @DisplayName("R4: Turns a request without an answer into a failure")
    @Description("""
            Each row sends GET /speakers/0 to a stand-in for the service on localhost. The production time-out is
            5 s; here it is shorter, so that the silent row does not slow the build.
            """)
    @TableTest("""
        Scenario                | Service                                         | Time-out | Outcome?
        Service answers         | answers 404                                     | PT0.5S   | Answered
        Service gives no answer | {nothing on the port, accepts and stays silent} | PT0.5S   | Failed
        """)
    void turnsARequestWithoutAnAnswerIntoAFailure(StandInService service, Duration timeout, String outcome) {
        try (service) {
            WarmUpOutcome result = new HttpWarmUpExchange(service.uri(), timeout).send(UNKNOWN_SPEAKER);
            assertEquals(outcome, result.getClass().getSimpleName());
        }
    }

    @TypeConverter
    public static StandInService parseStandInService(String behaviour) {
        try {
            return switch (behaviour) {
                case "answers 404" -> StandInService.answering(404);
                case "nothing on the port" -> StandInService.nothingOnThePort();
                case "accepts and stays silent" -> StandInService.silent();
                default -> throw new IllegalArgumentException("unknown stand-in service: " + behaviour);
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A stand-in for the service on a free localhost port, closed after the row. */
    static final class StandInService implements AutoCloseable {

        private final URI uri;
        private final AutoCloseable server;

        private StandInService(int port, AutoCloseable server) {
            this.uri = URI.create("http://localhost:" + port);
            this.server = server;
        }

        static StandInService answering(int status) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
            });
            server.start();
            return new StandInService(server.getAddress().getPort(), () -> server.stop(0));
        }

        static StandInService nothingOnThePort() throws IOException {
            int port;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
            return new StandInService(port, () -> { });
        }

        /** Takes connections in its backlog and never reads or answers them. */
        static StandInService silent() throws IOException {
            ServerSocket socket = new ServerSocket(0);
            return new StandInService(socket.getLocalPort(), socket);
        }

        URI uri() {
            return uri;
        }

        @Override
        public void close() {
            try {
                server.close();
            } catch (Exception e) {
                throw new IllegalStateException("could not stop the stand-in service", e);
            }
        }
    }
}
