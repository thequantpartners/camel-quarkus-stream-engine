package com.quantpartners.streamengine.infrastructure.camel.routes;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class DeadLetterQueueRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:dead-letter-channel")
            .routeId("dead-letter-channel-route")
            .log("☠️ [DEAD LETTER CHANNEL] Inbound processing failed. Exception: ${exception.message}")
            .process(exchange -> {
                Throwable caught = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
                String errorMessage = (caught != null) ? caught.getMessage() : "Unknown stream processing fault";
                exchange.setHeader("DLQ_Error_Message", errorMessage);
                exchange.setHeader("DLQ_Timestamp", System.currentTimeMillis());
            })
            .log("📦 Stored poison pill in DLQ quarantine topic for inspection: ${header.DLQ_Error_Message}");
    }
}
