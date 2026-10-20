package com.quantpartners.streamengine.infrastructure.camel.processors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequestDto {
    public String eventId;
    public String idempotencyKey;
    public String accountId;
    public Double amount;
    public String currency;
    public Map<String, String> metadata;

    public TransactionRequestDto() {}

    public TransactionRequestDto(String eventId, String idempotencyKey, String accountId, Double amount, String currency, Map<String, String> metadata) {
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.metadata = metadata;
    }
}
