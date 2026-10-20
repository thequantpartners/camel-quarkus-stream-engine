package com.quantpartners.streamengine.application;

import com.quantpartners.streamengine.application.strategy.EventRoutingStrategy;
import com.quantpartners.streamengine.application.strategy.FraudEvaluationStrategy;
import com.quantpartners.streamengine.application.strategy.HighValuePaymentStrategy;
import com.quantpartners.streamengine.application.strategy.StandardEventStrategy;
import com.quantpartners.streamengine.application.usecase.ProcessEventUseCase;
import com.quantpartners.streamengine.domain.model.Money;
import com.quantpartners.streamengine.domain.model.RoutingDecision;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.domain.model.TransactionStatus;
import com.quantpartners.streamengine.domain.ports.AuditLedgerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Application Strategy & UseCase Tests")
class EventRoutingStrategyTest {

    private AuditLedgerPort auditLedgerMock;
    private ProcessEventUseCase processEventUseCase;

    @BeforeEach
    void setUp() {
        auditLedgerMock = Mockito.mock(AuditLedgerPort.class);
        when(auditLedgerMock.existsByIdempotencyKey(anyString())).thenReturn(false);

        List<EventRoutingStrategy> strategies = List.of(
            new FraudEvaluationStrategy(),
            new HighValuePaymentStrategy(),
            new StandardEventStrategy()
        );

        processEventUseCase = new ProcessEventUseCase(auditLedgerMock, strategies);
    }

    @Test
    @DisplayName("Should route to Fraud Alerts stream when metadata risk_level is CRITICAL")
    void shouldRouteToFraudWhenCritical() {
        TransactionEvent event = new TransactionEvent(
            "evt-001",
            "idem-key-001",
            "acc-001",
            Money.of(new BigDecimal("250.00"), Money.USD),
            TransactionStatus.PENDING,
            null,
            Map.of("risk_level", "CRITICAL")
        );

        RoutingDecision decision = processEventUseCase.evaluateRouting(event);

        assertThat(decision.destinationUri()).isEqualTo("kafka:transactions.fraud-alerts");
        assertThat(decision.updatedStatus()).isEqualTo(TransactionStatus.REJECTED_FRAUD);
        verify(auditLedgerMock).recordAudit(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should route to High Value stream when amount is >= 10,000 USD")
    void shouldRouteToHighValue() {
        TransactionEvent event = new TransactionEvent(
            "evt-002",
            "idem-key-002",
            "acc-002",
            Money.of(new BigDecimal("15000.00"), Money.USD),
            TransactionStatus.PENDING,
            null,
            Map.of()
        );

        RoutingDecision decision = processEventUseCase.evaluateRouting(event);

        assertThat(decision.destinationUri()).isEqualTo("kafka:transactions.high-value");
        assertThat(decision.updatedStatus()).isEqualTo(TransactionStatus.FLAGGED_HIGH_VALUE);
    }

    @Test
    @DisplayName("Should route to Standard stream when amount is below threshold and no fraud detected")
    void shouldRouteToStandard() {
        TransactionEvent event = new TransactionEvent(
            "evt-003",
            "idem-key-003",
            "acc-003",
            Money.of(new BigDecimal("85.50"), Money.USD),
            TransactionStatus.PENDING,
            null,
            Map.of()
        );

        RoutingDecision decision = processEventUseCase.evaluateRouting(event);

        assertThat(decision.destinationUri()).isEqualTo("kafka:transactions.processed");
        assertThat(decision.updatedStatus()).isEqualTo(TransactionStatus.PROCESSED);
    }
}
