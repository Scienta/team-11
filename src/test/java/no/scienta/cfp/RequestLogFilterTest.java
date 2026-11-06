package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

/**
 * The request log: one line per request in the container logs, so we can see what the harness sends.
 */
@ExtendWith(OutputCaptureExtension.class)
class RequestLogFilterTest {

    private final RequestLogFilter filter = new RequestLogFilter();

    @Test
    void logsMethodPathQueryStatusAndBodies(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = jsonRequest("POST", "/talks", "{\"title\":\"Hi\"}");
        request.setQueryString("sort=rating");

        filter.doFilter(request, new MockHttpServletResponse(), chainAnswering(201, "{\"id\":7}"));

        assertTrue(output.getOut().contains(
                "POST /talks?sort=rating -> 201 in "), output.getOut());
        assertTrue(output.getOut().contains(
                "request={\"title\":\"Hi\"} response={\"id\":7}"), output.getOut());
    }

    @Test
    void passesTheResponseBodyOnToTheClient() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(jsonRequest("GET", "/talks/7", ""), response, chainAnswering(200, "{\"id\":7}"));

        assertEquals("{\"id\":7}", response.getContentAsString());
    }

    @Test
    void shortensLongBodies(CapturedOutput output) throws Exception {
        String longTitle = "x".repeat(RequestLogFilter.MAX_BODY_CHARS + 500);

        filter.doFilter(jsonRequest("POST", "/talks", longTitle), new MockHttpServletResponse(),
                chainAnswering(400, ""));

        assertTrue(output.getOut().contains("…(+500 chars)"), output.getOut());
        assertFalse(output.getOut().contains(longTitle), "logged the whole body");
    }

    private static MockHttpServletRequest jsonRequest(String method, String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /** A chain that reads the request body, as a controller does, and answers with the given status and body. */
    private static MockFilterChain chainAnswering(int status, String body) {
        return new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
                StreamUtils.copyToString(req.getInputStream(), StandardCharsets.UTF_8);
                res.setStatus(status);
                res.setCharacterEncoding("UTF-8");
                res.getWriter().write(body);
            }
        });
    }
}
