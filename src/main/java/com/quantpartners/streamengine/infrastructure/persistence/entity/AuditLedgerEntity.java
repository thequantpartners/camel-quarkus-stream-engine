package com.quantpartners.streamengine.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_ledger", indexes = {
    @Index(name = "idx_audit_idempotency", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_audit_event_id", columnList = "event_id"),
    @Index(name = "idx_audit_status", columnList = "status")
})
public class AuditLedgerEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "event_id", nullable = false, length = 64)
    public String eventId;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    public String idempotencyKey;

    @Column(name = "account_id", nullable = false, length = 64)
    public String accountId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    public BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    public String currency;

    @Column(name = "status", nullable = false, length = 32)
    public String status;

    @Column(name = "destination_uri", nullable = false, length = 255)
    public String destinationUri;

    @Column(name = "execution_details", length = 512)
    public String executionDetails;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public AuditLedgerEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }
}
