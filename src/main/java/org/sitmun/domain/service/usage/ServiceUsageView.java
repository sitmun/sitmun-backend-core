package org.sitmun.domain.service.usage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceUsageView(
    boolean measured,
    Long total,
    Long failed,
    LocalDate lastUsedDay,
    long viewerLoads,
    List<UsagePoint> series,
    List<UsageOperationTotal> operations,
    List<UsageApplicationTotal> applications,
    Long previousTotal,
    Long previousFailed,
    Instant asOf) {}
