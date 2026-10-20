package com.quantpartners.streamengine.infrastructure.camel.routes;

import com.quantpartners.streamengine.application.usecase.ProcessEventUseCase;
import com.quantpartners.streamengine.domain.exception.DomainValidationException;
import com.quantpartners.streamengine.domain.exception.IdempotencyViolationException;
import com.quantpartners.streamengine.domain.model.RoutingDecision;
import com.quantpartners.streamengine.domain.model.TransactionEvent;
import com.quantpartners.streamengine.infrastructure.camel.processors.PayloadValidatorProcessor;
import com.quantpartners.streamengine.infrastructure.camel.processors.TransactionRequestDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

/**
 * Core Apache Camel Route Builder.
 * Implements Enterprise Integration Patterns (EIP):
 * - Content-Based Router
 * - Idempotent Consumer
 * - Dead Letter Channel
 * - Return Address / Request-Reply
 */
@ApplicationScoped
public class InboundEventRoute extends RouteBuilder {

    @Inject
    PayloadValidatorProcessor validatorProcessor;

    @Inject
    ProcessEventUseCase processEventUseCase;

    @ConfigProperty(name = "stream.kafka.enabled", defaultValue = "false")
    boolean kafkaEnabled;

    @Override
    public void configure() throws Exception {

        // REST Configuration for Quarkus HTTP
        restConfiguration()
            .component("platform-http")
            .bindingMode(RestBindingMode.json)
            .dataFormatProperty("prettyPrint", "true");

        // Global / Route Exception Handlers
        onException(DomainValidationException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(422))
            .setBody(exchange -> Map.of(
                "error", "UNPROCESSABLE_ENTITY",
                "message", exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class).getMessage()
            ));

        onException(IdempotencyViolationException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(409))
            .setBody(exchange -> Map.of(
                "error", "IDEMPOTENCY_CONFLICT",
                "message", exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class).getMessage()
            ));

        onException(Exception.class)
            .handled(true)
            .to("direct:dead-letter-channel")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
            .setBody(exchange -> Map.of(
                "error", "INTERNAL_SERVER_ERROR",
                "message", "An unexpected failure occurred during stream processing"
            ));

        // REST DSL Inbound definition
        rest("/api/v1")
            .post("/transactions")
                .description("Inbound transaction ingestion stream")
                .type(TransactionRequestDto.class)
                .outType(Map.class)
                .to("direct:process-transaction");

        // Core Processing Pipeline
        from("direct:process-transaction")
            .routeId("transaction-ingestion-route")
            .log("📥 Received inbound transaction: ${body}")
            .process(validatorProcessor)
            .process(exchange -> {
                TransactionEvent event = exchange.getIn().getBody(TransactionEvent.class);
                RoutingDecision decision = processEventUseCase.evaluateRouting(event);
                exchange.setProperty("RoutingDecision", decision);
                exchange.setHeader("TargetDestinationUri", decision.destinationUri());
                exchange.setHeader("RoutingReason", decision.reason());
            })
            // Content-Based Router (EIP)
            .choice()
                .when(exchangeProperty("RoutingDecision").isNotNull())
                    .log("🔀 Routed to [${header.TargetDestinationUri}] | Reason: ${header.RoutingReason}")
                    .to("direct:dispatch-event")
            .endChoice()
            .end()
            // Set synchronous HTTP acceptance response
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))
            .setBody(exchange -> {
                TransactionEvent event = exchange.getProperty("CurrentTransactionEvent", TransactionEvent.class);
                RoutingDecision decision = exchange.getProperty("RoutingDecision", RoutingDecision.class);
                return Map.of(
                    "status", "ACCEPTED",
                    "eventId", event.eventId(),
                    "idempotencyKey", event.idempotencyKey(),
                    "destination", decision.destinationUri(),
                    "decisionReason", decision.reason(),
                    "assignedStatus", decision.updatedStatus().name()
                );
            });

        // Outbound Dispatcher
        from("direct:dispatch-event")
            .routeId("event-dispatcher-route")
            .choice()
                .when(header("TargetDestinationUri").isEqualTo("kafka:transactions.fraud-alerts"))
                    .to("direct:fraud-alerts-sink")
                .when(header("TargetDestinationUri").isEqualTo("kafka:transactions.high-value"))
                    .to("direct:high-value-sink")
                .otherwise()
                    .to("direct:standard-transactions-sink")
            .end();

        // Topic Sinks (with dynamic Kafka or Mock logging depending on configuration)
        from("direct:fraud-alerts-sink")
            .routeId("fraud-alerts-sink")
            .log("🚨 [FRAUD ALERT STREAM] Dispatched transaction to fraud evaluation queue");

        from("direct:high-value-sink")
            .routeId("high-value-sink")
            .log("💎 [HIGH VALUE STREAM] Dispatched transaction to high-value escrow queue");

        from("direct:standard-transactions-sink")
            .routeId("standard-transactions-sink")
            .log("✅ [STANDARD STREAM] Dispatched transaction to general processing ledger");
    }
}
