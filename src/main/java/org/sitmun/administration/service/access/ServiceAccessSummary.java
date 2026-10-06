package org.sitmun.administration.service.access;

import java.time.Instant;
import java.util.List;
import org.sitmun.domain.service.usage.UsageWindow;

public record ServiceAccessSummary(
    Integer serviceId,
    String status,
    int statusRank,
    String observer,
    long elapsedMs,
    Instant observedAt,
    String detail,
    List<ServiceAccessHour> hours,
    UsageWindow usage30) {}
