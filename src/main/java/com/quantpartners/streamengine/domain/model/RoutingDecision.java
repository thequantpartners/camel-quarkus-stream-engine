package com.quantpartners.streamengine.domain.model;

public record RoutingDecision(
    String destinationUri,
    String reason,
    TransactionStatus updatedStatus,
    boolean requiresAudit
) {
    public static RoutingDecision toTopic(String topicUri, String reason, TransactionStatus status) {
        return new RoutingDecision(topicUri, reason, status, true);
    }

    public static RoutingDecision toDeadLetter(String reason) {
        return new RoutingDecision("direct:dead-letter-channel", reason, TransactionStatus.DLQ_ROUTED, true);
    }
}
