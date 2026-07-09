package org.sitmun.administration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvExportRequestDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportErrorDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportResponseDto;
import org.sitmun.administration.service.i18n.LiteralTranslationCsvService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiteralTranslationCsvController unit tests")
class LiteralTranslationCsvControllerTest {

  @Mock private LiteralTranslationCsvService service;

  private LiteralTranslationCsvController controller;

  @BeforeEach
  void setUp() {
    controller = new LiteralTranslationCsvController(service);
  }

  @Test
  @DisplayName("export returns CSV download response")
  void exportReturnsCsvDownloadResponse() {
    LiteralTranslationCsvExportRequestDto request = new LiteralTranslationCsvExportRequestDto();
    request.setTargetLanguage("es");
    request.setLiteralIds(List.of(1L, 2L));
    request.setFileName("literal-translations-es-selected.csv");

    when(service.exportCsv(any())).thenReturn("a,b\r\n".getBytes(StandardCharsets.UTF_8));

    ResponseEntity<byte[]> response = controller.export(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("a,b\r\n".getBytes(StandardCharsets.UTF_8));
    assertThat(response.getHeaders().getFirst("Content-Disposition"))
        .contains("literal-translations-es-selected.csv");
    verify(service).exportCsv(request);
  }

  @Test
  @DisplayName("import delegates multipart file and target language")
  void importDelegatesMultipartFileAndTargetLanguage() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "literal-translations.csv",
            "text/csv",
            "literal,translation\r\n".getBytes(StandardCharsets.UTF_8));
    LiteralTranslationCsvImportResponseDto dto =
        LiteralTranslationCsvImportResponseDto.builder()
            .targetLanguage("es")
            .totalRows(1)
            .unchangedRows(1)
            .sourceLanguages(List.of("ca"))
            .errors(List.of(new LiteralTranslationCsvImportErrorDto(2, "ca", "hola", "bad row")))
            .build();
    when(service.importCsv("es", file)).thenReturn(dto);

    ResponseEntity<LiteralTranslationCsvImportResponseDto> response =
        controller.importCsv("es", file);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(dto);
    verify(service).importCsv("es", file);
  }
}
