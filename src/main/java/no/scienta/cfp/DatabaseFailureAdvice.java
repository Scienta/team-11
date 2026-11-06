package no.scienta.cfp;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers a database failure with a fixed message, so that no SQL or driver text reaches the client (security
 * review, 2026-11-06). The details go to the log. Every other error keeps its answer.
 */
@RestControllerAdvice
public class DatabaseFailureAdvice {

    static final String MESSAGE = "the service could not complete the request";

    private static final Logger log = LoggerFactory.getLogger(DatabaseFailureAdvice.class);

    @ExceptionHandler(DataAccessException.class)
    public void databaseFailure(DataAccessException failure, HttpServletResponse response) throws IOException {
        log.error("database failure", failure);
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), MESSAGE);
    }
}
