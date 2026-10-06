package org.sitmun.domain.service.usage;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsageApplicationTotal(
    int applicationId, String name, Long requests, Long failed, Long viewerLoads) {}
