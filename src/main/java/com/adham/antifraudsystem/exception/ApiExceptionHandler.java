package com.adham.antifraudsystem.exception;

import com.adham.antifraudsystem.service.TransactionFeedbackService.DuplicateFeedbackException;
import com.adham.antifraudsystem.service.TransactionFeedbackService.InvalidFeedbackCombinationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import jakarta.validation.ConstraintViolationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail body = problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request validation failed"
        );
        body.setProperty("errors", errors);

        return handleExceptionInternal(
                exception,
                body,
                headers,
                status,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail body = problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "The request body is missing or malformed"
        );

        return handleExceptionInternal(
                exception,
                body,
                headers,
                status,
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception) {

        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request validation failed"
        );
    }

    @ExceptionHandler({
            EmptyResultDataAccessException.class,
            NoSuchElementException.class
    })
    ProblemDetail handleNotFound(RuntimeException exception) {
        String detail = exception instanceof NoSuchElementException
                ? exception.getMessage()
                : "The requested resource was not found";

        return problem(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                detail
        );
    }

    @ExceptionHandler({
            DuplicateKeyException.class,
            DuplicateFeedbackException.class,
            IllegalStateException.class
    })
    ProblemDetail handleConflict(RuntimeException exception) {
        String detail = exception instanceof DuplicateKeyException
                ? "The resource already exists"
                : exception.getMessage();

        return problem(
                HttpStatus.CONFLICT,
                "Resource conflict",
                detail
        );
    }

    @ExceptionHandler(InvalidFeedbackCombinationException.class)
    ProblemDetail handleUnprocessableFeedback(
            InvalidFeedbackCombinationException exception) {

        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Invalid feedback combination",
                exception.getMessage()
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );
        problem.setTitle(title);
        return problem;
    }
}
