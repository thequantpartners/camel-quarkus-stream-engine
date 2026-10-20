package com.quantpartners.streamengine.domain;

import com.quantpartners.streamengine.domain.exception.DomainValidationException;
import com.quantpartners.streamengine.domain.model.Money;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.domain.model.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Domain Invariants & Value Object Tests")
class TransactionEventTest {

    @Test
    @DisplayName("Should successfully construct Money Value Object with Banker's Rounding")
    void shouldConstructValidMoney() {
        Money money = Money.of(new BigDecimal("150.255"), Money.USD);
        // Banker's rounding to 2 decimal places (HALF_EVEN) -> 150.26
        assertThat(money.amount()).isEqualByComparingTo("150.26");
        assertThat(money.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when Money amount is negative")
    void shouldRejectNegativeMoney() {
        assertThatThrownBy(() -> Money.of(-50.00, "USD"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Should enforce valid idempotencyKey length in TransactionEvent")
    void shouldRejectShortIdempotencyKey() {
        Money money = Money.of(100.00, "USD");
        assertThatThrownBy(() -> new TransactionEvent(
            "evt-123",
            "short", // Less than 8 characters
            "acc-456",
            money,
            TransactionStatus.PENDING,
            null,
            Map.of()
        ))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("Idempotency key must be at least 8 characters");
    }

    @Test
    @DisplayName("Should reject blank accountId in TransactionEvent")
    void shouldRejectBlankAccountId() {
        Money money = Money.of(100.00, "USD");
        assertThatThrownBy(() -> new TransactionEvent(
            "evt-123",
            "valid-idempotency-key",
            "   ",
            money,
            TransactionStatus.PENDING,
            null,
            Map.of()
        ))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("Account ID cannot be blank");
    }
}
