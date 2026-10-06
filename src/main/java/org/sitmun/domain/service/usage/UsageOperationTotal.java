package org.sitmun.domain.service.usage;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsageOperationTotal(String operation, Long requests, Long failed) {}
