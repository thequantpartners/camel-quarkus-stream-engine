package com.quantpartners.streamengine.domain.ports;

import com.quantpartners.streamengine.domain.model.TransactionEvent;

import java.util.Optional;

public interface AuditLedgerPort {
    void recordAudit(TransactionEvent event, String routingDestination, String executionDetails);
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<TransactionEvent> findByIdempotencyKey(String idempotencyKey);
}
