package org.sitmun.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.web.dto.LanguageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/config/languages")
@Tag(name = "language", description = "list of languages")
@Validated
public class LanguageController {
  private final LanguageRepository languageRepository;

  public LanguageController(LanguageRepository languageRepository) {
    this.languageRepository = languageRepository;
  }

  @GetMapping
  public ResponseEntity<List<LanguageDTO>> getLanguages() {
    List<LanguageDTO> languages =
        this.languageRepository.findAll().stream()
            .map(this::languageToDTO)
            .collect(Collectors.toList());

    if (languages.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(languages);
  }

  private LanguageDTO languageToDTO(Language language) {
    return LanguageDTO.builder()
        .id(language.getId())
        .name(language.getName())
        .shortName(language.getShortname())
        .build();
  }
}
