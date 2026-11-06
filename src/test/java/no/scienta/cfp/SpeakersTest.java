package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Speaker registration: emails are unique regardless of letter case, also under concurrent registrations, and the
 * old admin search matches names regardless of case. Runs the real app on a random port with a throwaway database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpeakersTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void rejectsARegistrationWithoutARealEmail() throws Exception {
        assertEquals(400, register("Grace Hopper", "not-an-email").statusCode());
    }

    // Speakers R4: a character is one code point, so an emoji counts once, not as two UTF-16 units
    @ParameterizedTest(name = "{1} x {0} -> {2}")
    @CsvSource({
            "x,  500, 201",
            "x,  501, 400",
            "😀, 500, 201",
            "😀, 501, 400",
    })
    void limitsTheBioTo500Characters(String character, int length, int status) throws Exception {
        String json = "{\"name\":\"Grace Hopper\",\"email\":\"grace-" + UUID.randomUUID()
                + "@example.com\",\"bio\":\"" + character.repeat(length) + "\"}";
        HttpResponse<String> res = http.send(request("POST", "/speakers", json), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, res.statusCode(), res.body());
    }

    // Speakers decision: the admin update has the same 500-character bio limit as registration
    @ParameterizedTest(name = "update with {1} x {0} -> {2}")
    @CsvSource({
            "😀, 500, 200",
            "x,  501, 400",
    })
    void limitsTheBioTo500CharactersOnUpdate(String character, int length, int status) throws Exception {
        String email = "grace-" + UUID.randomUUID() + "@example.com";
        HttpResponse<String> created = register("Grace Hopper", email);
        String id = created.body().replaceAll(".*\"id\":(\\d+).*", "$1");
        String json = "{\"name\":\"Grace Hopper\",\"email\":\"" + email + "\",\"bio\":\""
                + character.repeat(length) + "\"}";
        HttpResponse<String> res = http.send(request("PUT", "/speakers/" + id, json), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, res.statusCode(), res.body().substring(0, Math.min(200, res.body().length())));
    }

    @Test
    void anEmailRegistersOnceRegardlessOfLetterCase() throws Exception {
        String email = "grace-" + UUID.randomUUID() + "@example.com";
        assertEquals(201, register("Grace Hopper", email).statusCode());
        assertEquals(409, register("Grace Hopper", email.toUpperCase()).statusCode());
    }

    @Test
    void concurrentRegistrationsOfOneEmailHaveOneWinner() throws Exception {
        String email = "racer-" + UUID.randomUUID() + "@example.com";
        List<CompletableFuture<HttpResponse<String>>> sent = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String variant = i % 2 == 0 ? email : email.toUpperCase();
            sent.add(http.sendAsync(request("POST", "/speakers", speaker("Racer " + i, variant)),
                    HttpResponse.BodyHandlers.ofString()));
        }
        int created = 0;
        for (CompletableFuture<HttpResponse<String>> f : sent) {
            HttpResponse<String> res = f.join();
            assertTrue(res.statusCode() == 201 || res.statusCode() == 409, res.statusCode() + " " + res.body());
            if (res.statusCode() == 201) {
                created++;
            }
        }
        assertEquals(1, created);
    }

    @Test
    void searchMatchesNamesRegardlessOfCase() throws Exception {
        String name = "Lovelace" + UUID.randomUUID().toString().substring(0, 8);
        assertEquals(201, register("Ada " + name, "ada-" + UUID.randomUUID() + "@example.com").statusCode());

        HttpResponse<String> found = send("GET", "/speakers/search?q=" + name.toLowerCase());
        assertEquals(200, found.statusCode());
        assertTrue(found.body().contains("\"name\":\"Ada " + name + "\""), found.body());
    }

    private HttpResponse<String> register(String name, String email) throws Exception {
        return http.send(request("POST", "/speakers", speaker(name, email)), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(String method, String path) throws Exception {
        return http.send(request(method, path, null), HttpResponse.BodyHandlers.ofString());
    }

    private static String speaker(String name, String email) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}";
    }

    private HttpRequest request(String method, String path, String json) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
}
