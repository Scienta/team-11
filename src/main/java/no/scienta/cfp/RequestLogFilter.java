package no.scienta.cfp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Writes one log line per HTTP request: method, path and query, status, duration, and both bodies (shortened, with
 * each email masked).
 * The harness checks show up in the container logs only through these lines.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class RequestLogFilter extends OncePerRequestFilter {

    static final int MAX_BODY_CHARS = 2000;
    /** The request log never holds more of a request body than this. */
    static final int MAX_CACHED_BODY_BYTES = 8000;

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);
    private static final String NOT_IN_AN_EMAIL = "[^\\s@\"'\\\\,;:<>()\\[\\]{}]";
    private static final Pattern EMAIL = Pattern.compile(
            "(" + NOT_IN_AN_EMAIL + ")" + NOT_IN_AN_EMAIL + "{0,63}@(" + NOT_IN_AN_EMAIL + "{1,255})");
    /** The longest email: 64 characters before the @, and 255 after it. */
    private static final int LONGEST_EMAIL_CHARS = 64 + 1 + 255;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, MAX_CACHED_BODY_BYTES);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        long started = System.nanoTime();
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long millis = (System.nanoTime() - started) / 1_000_000;
            log.info("{} {} -> {} in {} ms request={} response={}",
                    request.getMethod(), pathWithQuery(request), cachedResponse.getStatus(), millis,
                    shortened(cachedRequest.getContentAsByteArray()),
                    shortened(cachedResponse.getContentAsByteArray()));
            cachedResponse.copyBodyToResponse();
        }
    }

    private static String pathWithQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    /**
     * The start of the body, on one line and with each email masked. Only the start goes through the regular
     * expressions, so a large body costs no more time than a small one. The start includes room for one more email,
     * so the cut never leaves part of an email unmasked.
     */
    private static String shortened(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        String start = withMaskedEmails(onOneLine(firstChars(text, MAX_BODY_CHARS + LONGEST_EMAIL_CHARS)));
        if (text.length() <= MAX_BODY_CHARS) {
            return start;
        }
        return firstChars(start, MAX_BODY_CHARS) + "…(+" + (text.length() - MAX_BODY_CHARS) + " chars)";
    }

    private static String firstChars(String text, int count) {
        return text.length() <= count ? text : text.substring(0, count);
    }

    private static String onOneLine(String text) {
        return text.replaceAll("\\s*[\\r\\n]+\\s*", " ");
    }

    /** Keeps the first character and the domain of each email: "ada@example.com" becomes "a***@example.com". */
    private static String withMaskedEmails(String text) {
        return EMAIL.matcher(text).replaceAll("$1***@$2");
    }
}
