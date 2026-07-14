package org.sitmun.administration.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.service.LanguageAdministrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
public class LanguageAdminController {

  private final LanguageAdministrationService languageAdministrationService;

  @PostMapping("/reorder")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> reorder(@RequestBody List<Integer> languageIds) {
    languageAdministrationService.reorderLanguages(languageIds);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/default")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> setDefault(@PathVariable Integer id) {
    languageAdministrationService.setDefaultLanguage(id);
    return ResponseEntity.noContent().build();
  }
}
