package com.quantpartners.streamengine.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

@ApplicationScoped
@Readiness
@Liveness
public class EngineReadinessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("Quarkus-Camel-Stream-Engine")
            .up()
            .withData("runtime", "Quarkus 3.x + Camel EIP")
            .withData("jvm", System.getProperty("java.version"))
            .withData("memoryFreeMB", Runtime.getRuntime().freeMemory() / (1024 * 1024))
            .build();
    }
}
