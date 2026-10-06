package org.sitmun.domain.service.usage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsageWindow(
    boolean measured,
    Long total,
    Long failed,
    LocalDate lastUsedDay,
    long viewerLoads,
    List<Long> days) {}
