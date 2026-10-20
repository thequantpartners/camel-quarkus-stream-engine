package com.quantpartners.streamengine.infrastructure.camel.processors;

import com.quantpartners.streamengine.domain.exception.DomainValidationException;
import com.quantpartners.streamengine.domain.model.Money;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.domain.model.TransactionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

@ApplicationScoped
public class PayloadValidatorProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        TransactionRequestDto dto = exchange.getIn().getBody(TransactionRequestDto.class);

        if (dto == null) {
            throw new DomainValidationException("Request body cannot be null or empty");
        }

        if (dto.amount == null) {
            throw new DomainValidationException("Transaction amount is required");
        }

        Currency currency;
        try {
            currency = (dto.currency == null || dto.currency.isBlank()) 
                ? Currency.getInstance("USD") 
                : Currency.getInstance(dto.currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("Invalid ISO 4217 currency code: " + dto.currency);
        }

        Money money = Money.of(BigDecimal.valueOf(dto.amount), currency);

        TransactionEvent event = new TransactionEvent(
            dto.eventId,
            dto.idempotencyKey,
            dto.accountId,
            money,
            TransactionStatus.PENDING,
            Instant.now(),
            dto.metadata
        );

        exchange.getIn().setBody(event);
        exchange.setProperty("CurrentTransactionEvent", event);
    }
}
