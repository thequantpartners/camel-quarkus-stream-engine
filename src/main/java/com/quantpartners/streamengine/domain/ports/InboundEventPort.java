package com.quantpartners.streamengine.domain.ports;

import com.quantpartners.streamengine.domain.model.RoutingDecision;
import com.quantpartners.streamengine.domain.model.TransactionEvent;

public interface InboundEventPort {
    RoutingDecision evaluateRouting(TransactionEvent event);
}
