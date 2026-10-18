package com.auction.app.web.error;

import com.auction.app.web.security.IncompleteIdentityException;
import com.auction.domain.auction.AuctionConcurrentlyModifiedException;
import com.auction.domain.auction.AuctionNotFoundException;
import com.auction.domain.auction.AuctionNotOpenForBiddingException;
import com.auction.domain.auction.AuctionNotRunningException;
import com.auction.domain.auction.BidTooLowException;
import com.auction.domain.auction.OnlyBuyerCanBidException;
import com.auction.domain.auction.OnlySellerCanCreateAuctionException;
import com.auction.domain.auction.SellerCannotBidOnOwnAuctionException;
import com.auction.domain.shared.DomainException;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.user.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Maps every error to an RFC 9457 {@link ProblemDetail}. Spring MVC errors (malformed JSON, missing header,
 * type mismatch, invalid paging) are handled by the base class as 400.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_PREFIX = "urn:bidanddrive:problem:";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed",
                "The request contains invalid fields.");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail invalidValue(DomainException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-value", "Invalid value", ex.getMessage());
    }

    @ExceptionHandler({OnlySellerCanCreateAuctionException.class, OnlyBuyerCanBidException.class,
            SellerCannotBidOnOwnAuctionException.class})
    ProblemDetail forbidden(DomainException ex) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Action not allowed for this user", ex.getMessage());
    }

    @ExceptionHandler(IncompleteIdentityException.class)
    ProblemDetail incompleteIdentity(IncompleteIdentityException ex) {
        return problem(HttpStatus.FORBIDDEN, "incomplete-identity", "Incomplete identity", ex.getMessage());
    }

    @ExceptionHandler({AuctionNotFoundException.class, UserNotFoundException.class})
    ProblemDetail notFound(DomainException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(BidTooLowException.class)
    ProblemDetail bidTooLow(DomainException ex) {
        return problem(HttpStatus.CONFLICT, "bid-too-low", "Bid too low", ex.getMessage());
    }

    @ExceptionHandler({AuctionNotOpenForBiddingException.class, AuctionNotRunningException.class})
    ProblemDetail auctionNotOpen(DomainException ex) {
        return problem(HttpStatus.CONFLICT, "auction-not-open", "Auction not open", ex.getMessage());
    }

    @ExceptionHandler(AuctionConcurrentlyModifiedException.class)
    ProblemDetail concurrentModification(DomainException ex) {
        return problem(HttpStatus.CONFLICT, "concurrent-modification", "Concurrent update", ex.getMessage());
    }

    /** Safety net if a version conflict escapes the adapter untranslated; its message names internal classes. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail optimisticLockFailure(ObjectOptimisticLockingFailureException ex) {
        log.warn("Untranslated optimistic lock failure", ex);
        return problem(HttpStatus.CONFLICT, "concurrent-modification", "Concurrent update",
                "the resource was modified concurrently, reload and retry");
    }

    /** Any other domain rule we forgot to map: a client error, not a server crash. */
    @ExceptionHandler(DomainException.class)
    ProblemDetail otherDomainRule(DomainException ex) {
        log.warn("Unmapped domain exception {}", ex.getClass().getName());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "business-rule", "Business rule violated", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + type));
        problem.setTitle(title);
        return problem;
    }
}
