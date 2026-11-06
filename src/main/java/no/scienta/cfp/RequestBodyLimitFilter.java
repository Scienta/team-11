package no.scienta.cfp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses a request body larger than 1 MB with 413, before the service reads it into memory (security review,
 * 2026-11-06). It counts the bytes itself, so a chunked body without a Content-Length is limited too. Runs inside
 * {@link RequestLogFilter}, so the log shows each refusal.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    static final int MAX_BODY_BYTES = 1024 * 1024;

    private static final String MESSAGE = "the request body is larger than 1 MB";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), MESSAGE);
            return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), MESSAGE);
            return;
        }
        chain.doFilter(new ReadBodyRequest(request, body), response);
    }

    /** The request with its body already read, so the rest of the chain reads it from memory. */
    private static final class ReadBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        ReadBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bytes = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bytes.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("the body is already read");
                }

                @Override
                public int read() {
                    return bytes.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return bytes.read(buffer, offset, length);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
