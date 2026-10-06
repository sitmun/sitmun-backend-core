package org.sitmun.administration.service.access;

import java.time.Instant;

public record ServiceAccessSample(
    Instant observedAt, long elapsedMs, String status, int statusRank, String observer) {}
