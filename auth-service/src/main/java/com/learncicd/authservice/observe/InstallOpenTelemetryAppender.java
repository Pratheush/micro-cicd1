package com.learncicd.authservice.observe;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * This class is a Spring component that ensures the OpenTelemetry Logback appender is installed and active when your application starts:
 * - @Component → Spring will detect and register this bean automatically.
 * - Implements InitializingBean → The afterPropertiesSet() method runs once the bean is fully initialized by Spring.
 * - OpenTelemetryAppender.install(this.openTelemetry) → This call wires the OpenTelemetry Logback appender into the logging system, so every log entry is enriched with trace context (traceId, spanId) and exported via OTLP to Loki.
 * - Without this class, your logback-spring.xml OTEL appender definition exists, but it may not be automatically installed into the logging pipeline.
 * - This class guarantees that the OpenTelemetry appender is registered at startup, so logs are trace-aware and shipped to Loki.
 *
 * 👉 In short: this class is a bootstrap helper that makes sure the OpenTelemetry Logback appender is installed at runtime, so your logs are automatically correlated with distributed traces.
 *     From then on, every log.info(), log.warn(), etc. includes traceId/spanId and is exported to Loki.
 */
@Component
class InstallOpenTelemetryAppender implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
    }
}
