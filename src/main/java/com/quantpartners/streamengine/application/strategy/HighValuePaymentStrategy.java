package com.quantpartners.streamengine.application.strategy;

import com.quantpartners.streamengine.domain.model.Money;
import com.quantpartners.streamengine.domain.model.RoutingDecision;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.domain.model.TransactionStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;

@ApplicationScoped
public class HighValuePaymentStrategy implements EventRoutingStrategy {

    private static final BigDecimal THRESHOLD_USD = new BigDecimal("10000.00");

    @Override
    public boolean appliesTo(TransactionEvent event) {
        Money threshold = Money.of(THRESHOLD_USD, event.money().currency());
        return event.money().isGreaterThanOrEqual(threshold);
    }

    @Override
    public RoutingDecision execute(TransactionEvent event) {
        return RoutingDecision.toTopic(
            "kafka:transactions.high-value",
            "Transaction exceeds high-value threshold: " + event.money().amount(),
            TransactionStatus.FLAGGED_HIGH_VALUE
        );
    }

    @Override
    public int priority() {
        return 10;
    }
}
