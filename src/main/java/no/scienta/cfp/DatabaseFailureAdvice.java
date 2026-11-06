package no.scienta.cfp;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers a database failure with a fixed message, so that no SQL, driver or connection pool text reaches the client
 * (security review, 2026-11-06, and S17). A failure is a failed query, or a transaction that can't start because the
 * database does not answer. The details go to the log. Every other error keeps its answer.
 */
@RestControllerAdvice
public class DatabaseFailureAdvice {

    static final String MESSAGE = "the service could not complete the request";

    private static final Logger log = LoggerFactory.getLogger(DatabaseFailureAdvice.class);

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public void databaseFailure(RuntimeException failure, HttpServletResponse response) throws IOException {
        log.error("database failure", failure);
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), MESSAGE);
    }
}
