package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * Registration for a workshop: who can register, for which workshops, how many, and the organisers' list. Runs the
 * real app on a random port with a throwaway database, emptied before each test, so that the first talk of a row gets
 * ID 1.
 */
@DisplayName("Workshop registration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WorkshopRegistrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void startWithNoTalks() {
        jdbc.execute("TRUNCATE speaker, talk, rating, workshop_registration, talk_co_speaker RESTART IDENTITY");
    }

    @DisplayName("R1: Registers an attendee only with a name and a real email")
    @Description("""
            Each row registers for an accepted workshop. A blank cell leaves the field out of the
            body, and null sends JSON null. The email rule is the same as for speakers.
            """)
    @TableTest("""
        Scenario       | Name         | Email           | Status?
        Name and email | Ada Lovelace | ada@example.com | 201
        Empty name     | ''           | ada@example.com | 400
        Name of spaces | '   '        | ada@example.com | 400
        Name is null   | null         | ada@example.com | 400
        No name        |              | ada@example.com | 400
        Not an email   | Ada Lovelace | not-an-email    | 400
        Empty email    | Ada Lovelace | ''              | 400
        Email is null  | Ada Lovelace | null            | 400
        No email       | Ada Lovelace |                 | 400
        """)
    void registersAnAttendeeOnlyWithANameAndARealEmail(JsonValue name, JsonValue email, int status) throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        assertEquals(status, send("POST", registrationsPath(workshop), body(name, email)).join().statusCode());
    }

    @DisplayName("Answers 400 for a request it cannot use")
    @Description("""
            Each row submits and accepts one workshop first, which gets ID 1; there is no talk 2. A
            blank body sends no body at all. The body is checked before the workshop, so an unknown
            workshop with an unusable body gives 400.
            """)
    @TableTest("""
        Scenario                  | Method      | Workshop ID | Body                                                   | Status?
        No body                   | POST        | 1           |                                                        | 400
        Body that is not JSON     | POST        | 1           | '{"name": "Ada Lovelace"'                              | 400
        Body that is a list       | POST        | 1           | '[]'                                                   | 400
        ID that is not a number   | {POST, GET} | abc         | '{"name": "Ada Lovelace", "email": "ada@example.com"}' | 400
        Unknown workshop, no name | POST        | 2           | '{"email": "ada@example.com"}'                         | 400
        """)
    void answers400ForARequestItCannotUse(String method, String workshopId, String body, int status) throws Exception {
        talkWith("WORKSHOP", "ACCEPTED");
        assertEquals(status, send(method, "/workshops/" + workshopId + "/registrations", body).join().statusCode());
    }

    @DisplayName("R2: Takes registrations only for a workshop")
    @Description("""
            Each row submits and accepts one talk in the given format, which gets ID 1. Talk 2 does not
            exist.
            """)
    @TableTest("""
        Scenario       | Format                                     | Talk ID | Register? | List?
        A workshop     | WORKSHOP                                   | 1       | 201       | 200
        Another format | {LIGHTNING_10, LIGHTNING_20, PRESENTATION} | 1       | 404       | 404
        No such talk   | WORKSHOP                                   | 2       | 404       | 404
        """)
    void takesRegistrationsOnlyForAWorkshop(String format, long talkId, int register, int list) throws Exception {
        talkWith(format, "ACCEPTED");
        assertEquals(register, register(talkId, "Ada Lovelace", "ada@example.com").statusCode());
        assertEquals(list, send("GET", registrationsPath(talkId), null).join().statusCode());
    }

    @DisplayName("R3: Takes registrations only for an accepted workshop")
    @Description("""
            Each row brings a new workshop to the status through the acceptance API: two ratings
            before ACCEPTED, and an accepted workshop withdrawn by its speaker for WITHDRAWN.
            """)
    @TableTest("""
        Scenario     | Workshop Status                              | Status?
        Accepted     | ACCEPTED                                     | 201
        Not accepted | {SUBMITTED, WAITLISTED, REJECTED, WITHDRAWN} | 409
        """)
    void takesRegistrationsOnlyForAnAcceptedWorkshop(String workshopStatus, int status) throws Exception {
        long workshop = talkWith("WORKSHOP", workshopStatus);
        assertEquals(status, register(workshop, "Ada Lovelace", "ada@example.com").statusCode());
    }

    @DisplayName("R4: Keeps the attendees of a withdrawn workshop")
    @Description("""
            Each row registers the attendees for an accepted workshop, then its speaker withdraws it.
            """)
    @TableTest("""
        Scenario          | Registered Before                    | Listed After Withdrawal?
        Two attendees     | [ada@example.com, grace@example.com] | [ada@example.com, grace@example.com]
        Nobody registered | []                                   | []
        """)
    void keepsTheAttendeesOfAWithdrawnWorkshop(List<String> registeredBefore, List<String> listedAfterWithdrawal)
            throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        registerOneByOne(workshop, registeredBefore);
        withdrawWorkshop(workshop);
        assertEquals(listedAfterWithdrawal, emailsListedFor(workshop));
    }

    @DisplayName("R5: Takes at most 20 attendees for a workshop")
    @Description("""
            Each row registers attendees with different emails one by one, then registers one more.
            """)
    @TableTest("""
        Scenario      | Already Registered | Status? | Listed?
        One seat left | 19                 | 201     | 20
        No seat left  | 20                 | 409     | 20
        """)
    void takesAtMost20Attendees(int alreadyRegistered, int status, int listed) throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        registerOneByOne(workshop, emails(alreadyRegistered));
        assertEquals(status, register(workshop, "Late Comer", "late@example.com").statusCode());
        assertEquals(listed, emailsListedFor(workshop).size());
    }

    @DisplayName("R5: Keeps 20 seats when many register at the same time")
    @Description("""
            All registrations of a row are sent at the same time, each with a different email, to one
            accepted workshop with no attendees. The service uses at most 5 database connections.
            """)
    @TableTest("""
        Scenario            | Registering At Once | Registered? | Refused? | Listed?
        Exactly the seats   | 20                  | 20          | 0        | 20
        More than the seats | 30                  | 20          | 10       | 20
        """)
    void keeps20SeatsWhenManyRegisterAtTheSameTime(int registeringAtOnce, long registered, long refused,
                                                   int listed) throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        Map<Integer, Long> statuses = statusCounts(atTheSameTime(workshop, emails(registeringAtOnce)));
        assertEquals(registered, statuses.getOrDefault(201, 0L));
        assertEquals(refused, statuses.getOrDefault(409, 0L));
        assertEquals(listed, emailsListedFor(workshop).size());
    }

    @DisplayName("R6, R7: Registers an email once for each workshop")
    @Description("""
            Each row registers ada@example.com for workshop 1 first. Workshops 1 and 2 are both
            accepted.
            """)
    @TableTest("""
        Scenario                    | Second Email                       | Second Workshop | Status?
        Same email, any letter case | {ada@example.com, ADA@Example.com} | 1               | 409
        Another email               | grace@example.com                  | 1               | 201
        Same email, other workshop  | ada@example.com                    | 2               | 201
        """)
    void registersAnEmailOnceForEachWorkshop(String secondEmail, long secondWorkshop, int status) throws Exception {
        long first = talkWith("WORKSHOP", "ACCEPTED");
        talkWith("WORKSHOP", "ACCEPTED");
        assertEquals(201, register(first, "Ada Lovelace", "ada@example.com").statusCode());
        assertEquals(status, register(secondWorkshop, "Ada Lovelace", secondEmail).statusCode());
    }

    @DisplayName("R6: Registers one of the same email sent at the same time")
    @Description("""
            All copies of a row are sent at the same time to one accepted workshop with no attendees.
            Every second copy writes the email in capitals.
            """)
    @TableTest("""
        Scenario       | Copies | Registered? | Refused?
        A double-click | 2      | 1           | 1
        Many copies    | 10     | 1           | 9
        """)
    void registersOneOfTheSameEmailSentAtTheSameTime(int copies, long registered, long refused) throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        List<String> sameEmail = IntStream.range(0, copies)
                .mapToObj(copy -> copy % 2 == 0 ? "ada@example.com" : "ADA@EXAMPLE.COM")
                .collect(Collectors.toList());
        Map<Integer, Long> statuses = statusCounts(atTheSameTime(workshop, sameEmail));
        assertEquals(registered, statuses.getOrDefault(201, 0L));
        assertEquals(refused, statuses.getOrDefault(409, 0L));
    }

    @DisplayName("R8: Lists the attendees in the order they registered")
    @Description("""
            Each row registers the emails one by one, in the order of the list, for an accepted
            workshop. The list shows each email as the attendee typed it.
            """)
    @TableTest("""
        Scenario            | Registered In Order                                       | Listed?
        Three attendees     | [ada@example.com, grace@example.com, barbara@example.com] | [ada@example.com, grace@example.com, barbara@example.com]
        Email with capitals | [ADA@Example.com]                                         | [ADA@Example.com]
        Nobody              | []                                                        | []
        """)
    void listsTheAttendeesInTheOrderTheyRegistered(List<String> registeredInOrder, List<String> listed)
            throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        registerOneByOne(workshop, registeredInOrder);
        assertEquals(listed, emailsListedFor(workshop));
    }

    @DisplayName("R9: Frees the seat when an attendee unregisters")
    @Description("""
            Each row fills an accepted workshop with 20 attendees, then registers one more.
            """)
    @TableTest("""
        Scenario                | Unregisters One First | Next Registration? | Listed?
        An attendee unregisters | true                  | 201                | 20
        Nobody unregisters      | false                 | 409                | 20
        """)
    void freesTheSeatWhenAnAttendeeUnregisters(boolean unregistersOneFirst, int nextRegistration, int listed)
            throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        registerOneByOne(workshop, emails(20));
        unregisterFirstIf(unregistersOneFirst, workshop);
        assertEquals(nextRegistration, register(workshop, "Late Comer", "late@example.com").statusCode());
        assertEquals(listed, emailsListedFor(workshop).size());
    }

    @DisplayName("R9: Unregisters a registration once, from its own workshop")
    @Description("""
            Each row has two workshops: workshop 1 in the given status, and workshop 2 accepted. Ada
            registers for workshop 1 before it gets its status, and her registration gets ID 1. Talk
            9 does not exist. The list is the list of workshop 1 after the request.
            """)
    @TableTest("""
        Scenario               | Workshop 1 Status     | Unregistered Before | Workshop ID | Registration ID | Status? | Listed After?
        Ada unregisters        | {ACCEPTED, WITHDRAWN} | false               | 1           | 1               | 204     | []
        A second time          | ACCEPTED              | true                | 1           | 1               | 404     | []
        Path of other workshop | ACCEPTED              | false               | 2           | 1               | 404     | [ada@example.com]
        No such registration   | ACCEPTED              | false               | 1           | 2               | 404     | [ada@example.com]
        No such workshop       | ACCEPTED              | false               | 9           | 1               | 404     | [ada@example.com]
        """)
    void unregistersARegistrationOnceFromItsOwnWorkshop(String workshopStatus, boolean unregisteredBefore,
                                                        long workshopId, long registrationId, int status,
                                                        List<String> listedAfter) throws Exception {
        long first = talkWith("WORKSHOP", "ACCEPTED");
        talkWith("WORKSHOP", "ACCEPTED");
        registerOneByOne(first, List.of("ada@example.com"));
        withdrawIf("WITHDRAWN".equals(workshopStatus), first);
        unregisterIf(unregisteredBefore, first, 1);
        assertEquals(status, unregisterAs(workshopId, registrationId, "ada@example.com").statusCode());
        assertEquals(listedAfter, emailsListedFor(first));
    }

    @DisplayName("R6, R9: Registers an email again after it unregistered")
    @Description("""
            Each row registers ada@example.com for an accepted workshop first, then registers the
            same email again.
            """)
    @TableTest("""
        Scenario             | Unregisters First | Status?
        Ada unregistered     | true              | 201
        Ada still registered | false             | 409
        """)
    void registersAnEmailAgainAfterItUnregistered(boolean unregistersFirst, int status) throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        registerOneByOne(workshop, List.of("ada@example.com"));
        unregisterFirstIf(unregistersFirst, workshop);
        assertEquals(status, register(workshop, "Ada Lovelace", "ada@example.com").statusCode());
    }

    @DisplayName("R9: Gives a freed seat to one attendee only")
    @Description("""
            Each row fills an accepted workshop with 20 attendees and unregisters one. Then all the
            new registrations of the row are sent at the same time, each with a different email.
            """)
    @TableTest("""
        Scenario         | Registering At Once | Registered? | Refused? | Listed?
        Two for one seat | 2                   | 1           | 1        | 20
        Ten for one seat | 10                  | 1           | 9        | 20
        """)
    void givesAFreedSeatToOneAttendeeOnly(int registeringAtOnce, long registered, long refused, int listed)
            throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        registerOneByOne(workshop, emails(20));
        unregisterFirstIf(true, workshop);
        List<String> newcomers = IntStream.rangeClosed(1, registeringAtOnce)
                .mapToObj(n -> "newcomer-" + n + "@example.com").collect(Collectors.toList());
        Map<Integer, Long> statuses = statusCounts(atTheSameTime(workshop, newcomers));
        assertEquals(registered, statuses.getOrDefault(201, 0L));
        assertEquals(refused, statuses.getOrDefault(409, 0L));
        assertEquals(listed, emailsListedFor(workshop).size());
    }

    @DisplayName("R10: Unregisters only with the registration's email")
    @Description("""
            Each row registers ada@example.com for an accepted workshop, and her registration gets
            ID 1. Then it sends DELETE /workshops/1/registrations/1 with the body. A blank body sends
            no body at all. The list is the workshop's list after the request.
            """)
    @TableTest("""
        Scenario              | Body                                                             | Status? | Listed After?
        Ada's email           | {'{"email": "ada@example.com"}', '{"email": "ADA@Example.com"}'} | 204     | []
        Another email         | '{"email": "grace@example.com"}'                                 | 404     | [ada@example.com]
        Body without email    | '{}'                                                             | 404     | [ada@example.com]
        No body               |                                                                  | 400     | [ada@example.com]
        Body that is not JSON | '{"email":'                                                      | 400     | [ada@example.com]
        """)
    void unregistersOnlyWithTheRegistrationsEmail(String body, int status, List<String> listedAfter)
            throws Exception {
        long workshop = talkWith("WORKSHOP", "ACCEPTED");
        registerOneByOne(workshop, List.of("ada@example.com"));
        assertEquals(status, unregister(workshop, 1, body).statusCode());
        assertEquals(listedAfter, emailsListedFor(workshop));
    }

    // ------------------------------------------------------------------------------------------- fixture

    /** Submits a talk in the format for a new speaker and brings it to the status through the acceptance API. */
    private long talkWith(String format, String status) throws Exception {
        long speaker = id(send("POST", "/speakers", json(Map.of("name", "Grace Hopper",
                "email", "speaker-" + System.nanoTime() + "@example.com"))).join());
        long talk = id(send("POST", "/talks", json(Map.of("title", "Legacy is a feeling", "format", format,
                "level", "BEGINNER", "speakerId", speaker))).join());
        statusChange(status).accept(new Talk(talk, speaker));
        return talk;
    }

    private record Talk(long id, long speaker) {
    }

    private Consumer<Talk> statusChange(String status) {
        return switch (status) {
            case "SUBMITTED" -> talk -> { };
            case "ACCEPTED" -> this::accept;
            case "WAITLISTED", "REJECTED" -> talk -> decide(talk, status);
            case "WITHDRAWN" -> talk -> {
                accept(talk);
                withdraw(talk);
            };
            default -> throw new IllegalArgumentException("no fixture for status " + status);
        };
    }

    private void accept(Talk talk) {
        IntStream.rangeClosed(1, 2).forEach(reviewer -> expect(200,
                send("PUT", "/talks/" + talk.id() + "/ratings/" + reviewer, "{\"score\":4}").join()));
        decide(talk, "ACCEPTED");
    }

    private void decide(Talk talk, String decision) {
        expect(200, send("PUT", "/talks/" + talk.id() + "/status", "{\"status\":\"" + decision + "\"}").join());
    }

    private void withdraw(Talk talk) {
        expect(200, send("POST", "/talks/" + talk.id() + "/withdraw", "{\"speakerId\":" + talk.speaker() + "}").join());
    }

    /** Withdraws a workshop that the fixture created, as its own speaker. */
    private void withdrawWorkshop(long workshop) throws Exception {
        long speaker = JSON.readTree(send("GET", "/talks/" + workshop, null).join().body()).get("speakerId").asLong();
        withdraw(new Talk(workshop, speaker));
    }

    private HttpResponse<String> unregister(long workshop, long registrationId, String body) {
        return send("DELETE", registrationsPath(workshop) + "/" + registrationId, body).join();
    }

    private HttpResponse<String> unregisterAs(long workshop, long registrationId, String email) {
        return unregister(workshop, registrationId, JSON.createObjectNode().put("email", email).toString());
    }

    /** Unregisters Ada's registration, when the condition holds. */
    private void unregisterIf(boolean condition, long workshop, long registrationId) {
        if (condition) {
            expect(204, unregisterAs(workshop, registrationId, "ada@example.com"));
        }
    }

    /** Unregisters the attendee who registered first for the workshop, when the condition holds. */
    private void unregisterFirstIf(boolean condition, long workshop) throws Exception {
        if (condition) {
            JsonNode first = JSON.readTree(send("GET", registrationsPath(workshop), null).join().body()).get(0);
            expect(204, unregisterAs(workshop, first.get("id").asLong(), first.get("email").asText()));
        }
    }

    private void withdrawIf(boolean condition, long workshop) throws Exception {
        if (condition) {
            withdrawWorkshop(workshop);
        }
    }

    private void registerOneByOne(long workshop, List<String> emails) {
        emails.forEach(email -> expect(201, register(workshop, "Attendee", email)));
    }

    private List<CompletableFuture<HttpResponse<String>>> atTheSameTime(long workshop, List<String> emails) {
        return emails.stream()
                .map(email -> send("POST", registrationsPath(workshop), registration("Attendee", email)))
                .collect(Collectors.toList());
    }

    private static Map<Integer, Long> statusCounts(List<CompletableFuture<HttpResponse<String>>> sent) {
        return sent.stream().map(CompletableFuture::join)
                .collect(Collectors.groupingBy(HttpResponse::statusCode, Collectors.counting()));
    }

    private static List<String> emails(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(n -> "attendee-" + n + "@example.com")
                .collect(Collectors.toList());
    }

    private List<String> emailsListedFor(long workshop) throws Exception {
        HttpResponse<String> res = send("GET", registrationsPath(workshop), null).join();
        expect(200, res);
        return StreamSupport.stream(JSON.readTree(res.body()).spliterator(), false)
                .map(registration -> registration.get("email").asText())
                .collect(Collectors.toList());
    }

    private HttpResponse<String> register(long workshop, String name, String email) {
        return send("POST", registrationsPath(workshop), registration(name, email)).join();
    }

    private static String registration(String name, String email) {
        ObjectNode body = JSON.createObjectNode().put("name", name).put("email", email);
        return body.toString();
    }

    /** A value for one field of a JSON body: text, or JSON null. A field without a value is left out. */
    record JsonValue(String text) {

        boolean isNull() {
            return text == null;
        }
    }

    @TypeConverter
    public static JsonValue parseJsonValue(String cell) {
        return new JsonValue("null".equals(cell) ? null : cell);
    }

    private static String body(JsonValue name, JsonValue email) {
        ObjectNode body = JSON.createObjectNode();
        putIfPresent(body, "name", name);
        putIfPresent(body, "email", email);
        return body.toString();
    }

    private static void putIfPresent(ObjectNode body, String field, JsonValue value) {
        if (value == null) {
            return;
        }
        if (value.isNull()) {
            body.putNull(field);
        } else {
            body.put(field, value.text());
        }
    }

    private static String registrationsPath(long workshop) {
        return "/workshops/" + workshop + "/registrations";
    }

    private static String json(Map<String, Object> fields) throws Exception {
        return JSON.writeValueAsString(fields);
    }

    private static long id(HttpResponse<String> res) throws Exception {
        JsonNode body = JSON.readTree(res.body());
        assertEquals(201, res.statusCode(), res.body());
        return body.get("id").asLong();
    }

    private static void expect(int status, HttpResponse<String> res) {
        assertEquals(status, res.statusCode(), res.body());
    }

    private CompletableFuture<HttpResponse<String>> send(String method, String path, String json) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
