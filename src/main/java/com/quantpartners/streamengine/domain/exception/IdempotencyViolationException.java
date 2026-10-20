package com.quantpartners.streamengine.domain.exception;

public class IdempotencyViolationException extends RuntimeException {
    public IdempotencyViolationException(String idempotencyKey) {
        super("Duplicate transaction detected for idempotencyKey: " + idempotencyKey);
    }
}
