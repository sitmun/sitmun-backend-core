package org.sitmun.administration.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvExportRequestDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportResponseDto;
import org.sitmun.administration.service.i18n.LiteralTranslationCsvService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/literal-translations/csv")
@RequiredArgsConstructor
@Slf4j
public class LiteralTranslationCsvController {

  private final LiteralTranslationCsvService literalTranslationCsvService;

  @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<byte[]> export(@RequestBody LiteralTranslationCsvExportRequestDto request) {
    String targetLanguage = request.getTargetLanguage();
    String fileName =
        sanitizeFileName(request.getFileName(), targetLanguage, request.getLiteralIds());
    log.info(
        "CSV export requested - targetLanguage: {}, literalIds: {}, fileName: {}",
        targetLanguage,
        request.getLiteralIds() != null ? request.getLiteralIds().size() : "all",
        fileName);
    byte[] content = literalTranslationCsvService.exportCsv(request);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName).build().toString())
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(content);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<LiteralTranslationCsvImportResponseDto> importCsv(
      @RequestParam("targetLanguage") String targetLanguage,
      @RequestPart("file") MultipartFile file) {
    log.info(
        "CSV import requested - targetLanguage: {}, fileName: {}",
        targetLanguage,
        file != null ? file.getOriginalFilename() : "null");
    LiteralTranslationCsvImportResponseDto response =
        literalTranslationCsvService.importCsv(targetLanguage, file);
    log.info(
        "CSV import completed - targetLanguage: {}, totalRows: {}, created: {}L/{}T, updated: {}, emptied: {}, unchanged: {}, failed: {}",
        response.getTargetLanguage(),
        response.getTotalRows(),
        response.getCreatedLiterals(),
        response.getCreatedTranslations(),
        response.getUpdatedTranslations(),
        response.getEmptiedTranslations(),
        response.getUnchangedRows(),
        response.getFailedRows());
    return ResponseEntity.ok(response);
  }

  private String sanitizeFileName(String fileName, String targetLanguage, List<Long> ids) {
    String base = fileName;
    if (base == null || base.isBlank()) {
      String safeTargetLanguage =
          targetLanguage == null || targetLanguage.isBlank() ? "translations" : targetLanguage;
      base =
          "literal-translations-"
              + safeTargetLanguage
              + (ids != null && !ids.isEmpty() ? "-partial" : "")
              + ".csv";
    }
    return base.replaceAll("[^A-Za-z0-9._-]", "_");
  }
}
