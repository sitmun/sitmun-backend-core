package org.sitmun.administration.service.access;

import java.time.Instant;

public record DueService(Integer id, String serviceUrl, Instant observedAt) {}
