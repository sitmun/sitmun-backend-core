package org.sitmun.administration.service.access;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sitmun.service-check")
public record ServiceCheckProperties(
    @NotNull Boolean enabled,
    @NotNull Duration interval,
    @NotNull Duration dispatchInterval,
    @NotNull Integer maxInFlight,
    @NotNull Integer maxPerHost,
    @NotNull Duration timeout,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    @NotNull Duration sampleRetention,
    @NotNull Duration usageRetention,
    @NotNull Duration retentionInterval,
    @NotNull Integer overdueWarnFactor,
    @NotNull Duration viewerFlushInterval,
    @NotNull Integer detailMaxLength,
    @NotNull Integer exceptionTextMaxLength,
    @NotNull Integer scanMaxBytes) {}
