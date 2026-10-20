package com.quantpartners.streamengine.application.usecase;

import com.quantpartners.streamengine.application.strategy.EventRoutingStrategy;
import com.quantpartners.streamengine.domain.exception.IdempotencyViolationException;
import com.quantpartners.streamengine.domain.model.RoutingDecision;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.domain.ports.AuditLedgerPort;
import com.quantpartners.streamengine.domain.ports.InboundEventPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Core Use Case: Orchestrates idempotency check, polymorphic routing selection,
 * and transactional audit recording.
 */
@ApplicationScoped
public class ProcessEventUseCase implements InboundEventPort {

    private final AuditLedgerPort auditLedger;
    private final List<EventRoutingStrategy> strategies;

    @Inject
    public ProcessEventUseCase(AuditLedgerPort auditLedger, Instance<EventRoutingStrategy> strategyInstances) {
        this.auditLedger = Objects.requireNonNull(auditLedger, "AuditLedgerPort cannot be null");
        this.strategies = strategyInstances.stream()
            .sorted(Comparator.comparingInt(EventRoutingStrategy::priority))
            .toList();
    }

    // Secondary constructor for standalone testing
    public ProcessEventUseCase(AuditLedgerPort auditLedger, List<EventRoutingStrategy> strategies) {
        this.auditLedger = Objects.requireNonNull(auditLedger, "AuditLedgerPort cannot be null");
        this.strategies = strategies.stream()
            .sorted(Comparator.comparingInt(EventRoutingStrategy::priority))
            .toList();
    }

    @Override
    public RoutingDecision evaluateRouting(TransactionEvent event) {
        // Idempotency Gate
        if (auditLedger.existsByIdempotencyKey(event.idempotencyKey())) {
            throw new IdempotencyViolationException(event.idempotencyKey());
        }

        // Strategy Resolution
        EventRoutingStrategy selectedStrategy = strategies.stream()
            .filter(strategy -> strategy.appliesTo(event))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No applicable routing strategy found for event: " + event.eventId()));

        RoutingDecision decision = selectedStrategy.execute(event);

        // Record Audit
        auditLedger.recordAudit(
            event.withStatus(decision.updatedStatus()),
            decision.destinationUri(),
            decision.reason()
        );

        return decision;
    }
}
