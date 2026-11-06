package no.scienta.cfp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Writes one log line per HTTP request: method, path and query, status, duration, and both bodies (shortened).
 * The harness checks show up in the container logs only through these lines.
 */
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    static final int MAX_BODY_CHARS = 2000;

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
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

    private static String shortened(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8).replaceAll("\\s*[\\r\\n]+\\s*", " ");
        if (text.length() <= MAX_BODY_CHARS) {
            return text;
        }
        return text.substring(0, MAX_BODY_CHARS) + "…(+" + (text.length() - MAX_BODY_CHARS) + " chars)";
    }
}
