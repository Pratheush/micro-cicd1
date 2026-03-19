package com.learncicd.userservice.observe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry expects the W3C traceparent format :: traceparent: 00-<traceId>-<spanId>-01
 *
 * User‑Service → Project‑Bookmark (Feign call): traceId changes because the Feign client isn’t automatically
 * propagating the tracing headers into the downstream request.
 * SO SETTING UP TRACEPARENT HEADER TO PROPAGATE FROM USER-SERVICE TO PROJECT-BOOKMARK
 */
@Configuration
@Slf4j
public class FeignTracingConfig {

    private final io.micrometer.tracing.Tracer tracer;

    public FeignTracingConfig(io.micrometer.tracing.Tracer tracer) {
        this.tracer = tracer;
    }

    @Bean
    public feign.RequestInterceptor tracingInterceptor() {
        return requestTemplate -> {

            var span = tracer.currentSpan();

            if (span != null) {
                String traceId = span.context().traceId();
                String spanId = span.context().spanId();

                String traceparent = "00-" + traceId + "-" + spanId + "-01";

                requestTemplate.header("traceparent", traceparent);
            }
        };
    }
}