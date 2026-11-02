package com.prasad.paystream.api.controller;

import com.prasad.paystream.core.service.AccountNotFoundException;
import com.prasad.paystream.core.service.InsufficientFundsException;
import com.prasad.paystream.core.service.UnbalancedEntryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain failures to honest HTTP semantics (RFC 7807 problem details). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleNotFound(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail handleInsufficientFunds(InsufficientFundsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(UnbalancedEntryException.class)
    ProblemDetail handleUnbalanced(UnbalancedEntryException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail handleBadRequest(Exception e) {
        String detail = e instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getAllErrors().stream()
                        .map(error -> error.getDefaultMessage() == null ? "invalid request" : error.getDefaultMessage())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("Invalid request")
                : e.getMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
