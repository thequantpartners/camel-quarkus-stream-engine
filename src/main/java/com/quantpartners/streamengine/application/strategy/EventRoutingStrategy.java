package com.quantpartners.streamengine.application.strategy;

import com.quantpartners.streamengine.domain.model.RoutingDecision;
import com.quantpartners.streamengine.domain.model.TransactionEvent;

/**
 * Strategy Pattern (GoF): Allows polymorphic routing algorithms based on event payload.
 */
public interface EventRoutingStrategy {
    boolean appliesTo(TransactionEvent event);
    RoutingDecision execute(TransactionEvent event);
    int priority();
}
