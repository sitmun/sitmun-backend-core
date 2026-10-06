package org.sitmun.administration.service.access;

import java.util.List;

public record ServiceAccessSamples(long timeoutMs, List<ServiceAccessSample> samples) {}
