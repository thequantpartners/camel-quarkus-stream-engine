package com.quantpartners.streamengine.infrastructure.persistence.adapter;

import com.quantpartners.streamengine.domain.model.Money;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.domain.model.TransactionStatus;
import com.quantpartners.streamengine.domain.ports.AuditLedgerPort;
import com.quantpartners.streamengine.infrastructure.persistence.entity.AuditLedgerEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Currency;
import java.util.Optional;

@ApplicationScoped
public class PostgresAuditAdapter implements AuditLedgerPort {

    @Override
    @Transactional
    public void recordAudit(TransactionEvent event, String routingDestination, String executionDetails) {
        AuditLedgerEntity entity = new AuditLedgerEntity();
        entity.eventId = event.eventId();
        entity.idempotencyKey = event.idempotencyKey();
        entity.accountId = event.accountId();
        entity.amount = event.money().amount();
        entity.currency = event.money().currency().getCurrencyCode();
        entity.status = event.status().name();
        entity.destinationUri = routingDestination;
        entity.executionDetails = executionDetails;
        entity.persist();
    }

    @Override
    @Transactional
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return AuditLedgerEntity.count("idempotencyKey", idempotencyKey) > 0;
    }

    @Override
    @Transactional
    public Optional<TransactionEvent> findByIdempotencyKey(String idempotencyKey) {
        AuditLedgerEntity entity = AuditLedgerEntity.find("idempotencyKey", idempotencyKey).firstResult();
        if (entity == null) {
            return Optional.empty();
        }

        TransactionEvent event = new TransactionEvent(
            entity.eventId,
            entity.idempotencyKey,
            entity.accountId,
            Money.of(entity.amount, Currency.getInstance(entity.currency)),
            TransactionStatus.valueOf(entity.status),
            entity.createdAt,
            null
        );
        return Optional.of(event);
    }
}
