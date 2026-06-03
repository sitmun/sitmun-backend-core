package org.sitmun.administration.controller.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;

@JacksonXmlRootElement(localName = "templateExportRequest")
public record TemplateExportRequestDto(
    @NotBlank @JacksonXmlProperty(localName = "output") String output,
    @JacksonXmlProperty(localName = "template") String template,
    @JacksonXmlProperty(localName = "taskId") Long taskId) {}
