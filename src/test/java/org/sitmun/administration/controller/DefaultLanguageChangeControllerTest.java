package org.sitmun.administration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.dto.DefaultLanguageChangePreview;
import org.sitmun.administration.dto.DefaultLanguageChangeRequest;
import org.sitmun.administration.dto.DefaultLanguageChangeResult;
import org.sitmun.administration.dto.MissingTranslationDto;
import org.sitmun.administration.service.DefaultLanguageChangeService;
import org.sitmun.test.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DefaultLanguageChangeControllerTest extends BaseTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DefaultLanguageChangeService service;

  @Test
  void previewReturns200WithMissingTranslations() throws Exception {
    // Given
    var missing = List.of(new MissingTranslationDto("Application", 1, "Application.name", "Test"));
    var preview = new DefaultLanguageChangePreview("en", "ca", 10, 10, 8, 2, missing);

    when(service.preview(eq("en"), eq("ca"))).thenReturn(preview);

    // When/Then
    mockMvc
        .perform(
            post("/api/language-default/change-preview")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"en\",\"to\":\"ca\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentDefault").value("en"))
        .andExpect(jsonPath("$.requestedDefault").value("ca"))
        .andExpect(jsonPath("$.missingTranslations").value(2))
        .andExpect(jsonPath("$.missing").isArray());
  }

  @Test
  void applyReturns409WhenBlockedByMissingTranslations() throws Exception {
    // Given
    when(service.apply(any(DefaultLanguageChangeRequest.class)))
        .thenThrow(
            new IllegalStateException(
                "Cannot change default language: 5 missing translations found"));

    // When/Then
    mockMvc
        .perform(
            post("/api/language-default/change")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"from\":\"en\",\"to\":\"ca\",\"continueOnMissingTranslations\":false}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void applyReturns200OnSuccess() throws Exception {
    // Given
    var result = new DefaultLanguageChangeResult("en", "ca", 25, 23, 2, Collections.emptyList());

    when(service.apply(any(DefaultLanguageChangeRequest.class))).thenReturn(result);

    // When/Then
    mockMvc
        .perform(
            post("/api/language-default/change")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"from\":\"en\",\"to\":\"ca\",\"continueOnMissingTranslations\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.previousDefault").value("en"))
        .andExpect(jsonPath("$.currentDefault").value("ca"))
        .andExpect(jsonPath("$.backupUpserts").value(25))
        .andExpect(jsonPath("$.restoredValues").value(23))
        .andExpect(jsonPath("$.preservedValues").value(2));
  }

  @Test
  void applyReturns400WhenInvalidLanguages() throws Exception {
    // Given
    when(service.apply(any(DefaultLanguageChangeRequest.class)))
        .thenThrow(new IllegalArgumentException("Source language not found: xx"));

    // When/Then
    mockMvc
        .perform(
            post("/api/language-default/change")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"from\":\"xx\",\"to\":\"ca\",\"continueOnMissingTranslations\":false}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void previewReturns400WhenInvalidLanguage() throws Exception {
    // Given
    when(service.preview(eq("xx"), eq("ca")))
        .thenThrow(new IllegalArgumentException("Source language not found: xx"));

    // When/Then
    mockMvc
        .perform(
            post("/api/language-default/change-preview")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"xx\",\"to\":\"ca\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }
}
