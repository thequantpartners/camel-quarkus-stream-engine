package com.quantpartners.streamengine.domain.model;

import com.quantpartners.streamengine.domain.exception.DomainValidationException;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Enterprise Core Domain Entity / Record.
 * Guarantees zero invalid instances through compact constructor invariants.
 */
public record TransactionEvent(
    String eventId,
    String idempotencyKey,
    String accountId,
    Money money,
    TransactionStatus status,
    Instant timestamp,
    Map<String, String> metadata
) {
    public TransactionEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
        if (idempotencyKey == null || idempotencyKey.trim().length() < 8) {
            throw new DomainValidationException(
                "Idempotency key must be at least 8 characters long"
            );
        }
        if (accountId == null || accountId.isBlank()) {
            throw new DomainValidationException("Account ID cannot be blank");
        }
        Objects.requireNonNull(money, "Money object cannot be null");
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        metadata = (metadata == null) ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    public TransactionEvent withStatus(TransactionStatus newStatus) {
        return new TransactionEvent(
            this.eventId,
            this.idempotencyKey,
            this.accountId,
            this.money,
            newStatus,
            this.timestamp,
            this.metadata
        );
    }
}
