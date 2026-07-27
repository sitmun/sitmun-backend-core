package org.sitmun.administration.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.sitmun.domain.DomainConstants;

public record TemplateExportRequestDto(
    @NotBlank @Pattern(regexp = "(?i)\\s*pdf\\s*") String output,
    @NotBlank @Size(max = DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_CHARACTERS)
        String template,
    @Positive Integer taskId,
    @Positive Integer templateTaskId,
    @Positive Integer applicationId,
    @Positive Integer territoryId) {}
