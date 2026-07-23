package org.sitmun.administration.controller;

import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.LiteralTranslationListItemDto;
import org.sitmun.administration.controller.dto.LiteralTranslationUpsertRequestDto;
import org.sitmun.administration.service.i18n.LiteralTranslationCrudService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/literal-translations")
@RequiredArgsConstructor
public class LiteralTranslationController {

  private final LiteralTranslationCrudService literalTranslationCrudService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PagedModel<LiteralTranslationListItemDto>> list(
      @RequestParam("lang") String language,
      Pageable pageable,
      @RequestParam(value = "filter", required = false) String filter,
      @RequestParam(value = "searchText", required = false) String searchText) {
    return ResponseEntity.ok(
        new PagedModel<>(
            literalTranslationCrudService.list(language, filter, searchText, pageable)));
  }

  @GetMapping("/{lang}/completion")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Double> getLanguageCompletionPct(@PathVariable final String lang) {
    return new ResponseEntity<>(
        literalTranslationCrudService.getLanguageCompletionPct(lang), HttpStatus.OK);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<LiteralTranslationListItemDto> create(
      @RequestBody LiteralTranslationUpsertRequestDto requestDto) {
    return ResponseEntity.ok(literalTranslationCrudService.create(requestDto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<LiteralTranslationListItemDto> update(
      @PathVariable Integer id, @RequestBody LiteralTranslationUpsertRequestDto requestDto) {
    return ResponseEntity.ok(literalTranslationCrudService.update(id, requestDto));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Integer id) {
    literalTranslationCrudService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
