package com.quantpartners.streamengine.application.strategy;

import com.quantpartners.streamengine.domain.model.RoutingDecision;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.domain.model.TransactionStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FraudEvaluationStrategy implements EventRoutingStrategy {

    @Override
    public boolean appliesTo(TransactionEvent event) {
        String riskTag = event.metadata().get("risk_level");
        return "CRITICAL".equalsIgnoreCase(riskTag) || "BLOCKED".equalsIgnoreCase(riskTag);
    }

    @Override
    public RoutingDecision execute(TransactionEvent event) {
        return RoutingDecision.toTopic(
            "kafka:transactions.fraud-alerts",
            "Suspicious risk tag detected: " + event.metadata().get("risk_level"),
            TransactionStatus.REJECTED_FRAUD
        );
    }

    @Override
    public int priority() {
        return 1; // Highest priority
    }
}
