package com.quantpartners.streamengine.application.strategy;

import com.quantpartners.streamengine.domain.model.RoutingDecision;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.domain.model.TransactionStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StandardEventStrategy implements EventRoutingStrategy {

    @Override
    public boolean appliesTo(TransactionEvent event) {
        return true; // Fallback default strategy
    }

    @Override
    public RoutingDecision execute(TransactionEvent event) {
        return RoutingDecision.toTopic(
            "kafka:transactions.processed",
            "Standard business processing stream",
            TransactionStatus.PROCESSED
        );
    }

    @Override
    public int priority() {
        return 100; // Lowest priority
    }
}
