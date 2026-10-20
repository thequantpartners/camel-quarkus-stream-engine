package com.quantpartners.streamengine.domain.model;

public enum TransactionStatus {
    PENDING,
    PROCESSED,
    FLAGGED_HIGH_VALUE,
    REJECTED_FRAUD,
    DLQ_ROUTED
}
