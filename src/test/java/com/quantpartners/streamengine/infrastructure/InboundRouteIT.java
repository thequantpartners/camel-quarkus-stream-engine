package com.quantpartners.streamengine.infrastructure;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("Inbound Route HTTP Integration Tests")
class InboundRouteIT {

    @Test
    @DisplayName("POST /api/v1/transactions - Should accept standard transaction (HTTP 202)")
    void shouldAcceptStandardTransaction() {
        String idempotencyKey = "idem-" + UUID.randomUUID();

        Map<String, Object> payload = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "idempotencyKey", idempotencyKey,
            "accountId", "acc-client-789",
            "amount", 125.50,
            "currency", "USD",
            "metadata", Map.of("channel", "MOBILE_APP")
        );

        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(202)
            .body("status", equalTo("ACCEPTED"))
            .body("idempotencyKey", equalTo(idempotencyKey))
            .body("assignedStatus", equalTo("PROCESSED"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions - Should reject duplicate transaction (HTTP 409 Conflict)")
    void shouldRejectDuplicateTransaction() {
        String idempotencyKey = "idem-duplicate-" + UUID.randomUUID();

        Map<String, Object> payload = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "idempotencyKey", idempotencyKey,
            "accountId", "acc-client-999",
            "amount", 50.00,
            "currency", "USD"
        );

        // First attempt -> 202 Accepted
        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(202);

        // Second attempt with same idempotency key -> 409 Conflict
        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(409)
            .body("error", equalTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions - Should reject invalid payload with negative amount (HTTP 422)")
    void shouldRejectNegativeAmount() {
        Map<String, Object> payload = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "idempotencyKey", "idem-invalid-key",
            "accountId", "acc-client-000",
            "amount", -10.00,
            "currency", "USD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(422)
            .body("error", equalTo("UNPROCESSABLE_ENTITY"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions - Should route high-value transaction to high-value stream")
    void shouldRouteHighValueTransaction() {
        String idempotencyKey = "idem-high-" + UUID.randomUUID();

        Map<String, Object> payload = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "idempotencyKey", idempotencyKey,
            "accountId", "acc-vip-001",
            "amount", 25000.00,
            "currency", "USD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(202)
            .body("assignedStatus", equalTo("FLAGGED_HIGH_VALUE"))
            .body("destination", equalTo("kafka:transactions.high-value"));
    }
}
