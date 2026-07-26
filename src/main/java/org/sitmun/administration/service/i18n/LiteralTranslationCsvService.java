package org.sitmun.administration.service.i18n;

import static org.sitmun.domain.PersistenceConstants.LONG_DESCRIPTION;

import com.opencsv.CSVReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.BOMInputStream;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvExportRequestDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportErrorDto;
import org.sitmun.administration.controller.dto.LiteralTranslationCsvImportResponseDto;
import org.sitmun.administration.service.csv.AbstractOpenCsvService;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiteralTranslationCsvService extends AbstractOpenCsvService {

  private static final String[] HEADER = {"source_language", "literal", "translation"};

  private final LiteralTranslationRepository literalTranslationRepository;
  private final LiteralTranslationValueRepository literalTranslationValueRepository;
  private final LanguageRepository languageRepository;
  private final PlatformTransactionManager transactionManager;

  @Transactional(readOnly = true)
  public byte[] exportCsv(LiteralTranslationCsvExportRequestDto request) {
    final String targetLanguageCode = normalizeRequiredCode(request.getTargetLanguage());
    if (!StringUtils.hasText(targetLanguageCode)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target language is required");
    }
    final Language targetLanguage =
        languageRepository
            .findByShortname(targetLanguageCode)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language not found"));
    final List<Long> literalIds =
        request.getLiteralIds() == null ? List.of() : request.getLiteralIds();

    log.info(
        "Exporting CSV - language: {}, literalIds: {}",
        targetLanguageCode,
        literalIds.isEmpty() ? "all" : literalIds.size());

    StringWriter writer = new StringWriter();
    try (var csvWriter = createWriter(writer)) {
      csvWriter.writeNext(HEADER, true);
      final List<ExportRow> rows = loadExportRows(targetLanguage.getId(), literalIds);
      rows.forEach(
          row ->
              csvWriter.writeNext(
                  new String[] {row.sourceLanguage(), row.literal(), row.translation()}, true));
      csvWriter.flush();
      log.info("CSV export complete - language: {}, rows: {}", targetLanguageCode, rows.size());
    } catch (Exception e) {
      log.error("CSV export failed - language: {}", targetLanguageCode, e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Unable to export CSV", e);
    }
    return withUtf8Bom(writer.toString());
  }

  public LiteralTranslationCsvImportResponseDto importCsv(
      String targetLanguageCode, MultipartFile file) {
    String targetLanguage = normalizeRequiredCode(targetLanguageCode);
    if (!StringUtils.hasText(targetLanguage)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target language is required");
    }
    if (file == null || file.isEmpty()) {
      log.warn("CSV import attempted with empty/null file - language: {}", targetLanguage);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is required");
    }
    final Language targetLanguageEntity =
        languageRepository
            .findByShortname(targetLanguage)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language not found"));

    log.info("Importing CSV - language: {}, file: {}", targetLanguage, file.getOriginalFilename());

    final CsvInspection inspection = readAndInspectRows(file);
    if (inspection.totalRows == 0) {
      log.info("CSV import - empty file (header only), language: {}", targetLanguage);
      return LiteralTranslationCsvImportResponseDto.builder()
          .targetLanguage(targetLanguage)
          .existingKeysNotInCsv(literalTranslationRepository.count())
          .sourceLanguages(List.of())
          .errors(List.of())
          .build();
    }

    log.info(
        "CSV import - parsed {} rows, valid rows: {}, source languages: {}, unique literals: {}",
        inspection.totalRows,
        inspection.validRows.size(),
        inspection.sourceLanguages,
        inspection.literals.size());

    final Map<String, Integer> languageIds = resolveLanguageIds(inspection.sourceLanguages);
    final List<ValidatedCsvRow> processableRows = new ArrayList<>();
    final List<LiteralTranslationCsvImportErrorDto> errors = new ArrayList<>(inspection.errors);

    inspection.validRows.forEach(
        row -> {
          final Integer sourceLanguageId = languageIds.get(row.sourceLanguage());
          if (sourceLanguageId == null) {
            errors.add(
                new LiteralTranslationCsvImportErrorDto(
                    row.rowNumber(),
                    row.sourceLanguage(),
                    row.literal(),
                    "One or more source languages do not exist"));
            return;
          }
          processableRows.add(row.withSourceLanguageId(sourceLanguageId));
        });

    final Set<String> processableLiterals = literalsOf(processableRows);

    final Map<String, ExistingLiteral> existingByLiteral =
        loadExistingLiterals(processableLiterals);
    final Map<Integer, String> existingTargetValues =
        loadExistingTargetValues(targetLanguageEntity.getId(), existingByLiteral.values());
    long existingLiteralsBefore = literalTranslationRepository.count();
    long existingLiteralsMatchedBefore = existingByLiteral.size();

    log.info(
        "CSV import - existing literals matched: {}/{}",
        existingByLiteral.size(),
        processableLiterals.size());

    final Summary summary =
        new Summary(targetLanguage, sourceLanguagesOf(processableRows), inspection.totalRows);

    final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

    processableRows.forEach(
        row -> {
          try {
            RowOutcome outcome =
                transactionTemplate.execute(
                    status ->
                        processRow(
                            row, targetLanguageEntity, existingByLiteral, existingTargetValues));
            if (outcome == null) {
              return;
            }
            applyOutcome(summary, existingByLiteral, existingTargetValues, outcome);
          } catch (RuntimeException e) {
            errors.add(toImportError(row.rowNumber(), row.sourceLanguage(), row.literal(), e));
          }
        });

    summary.failedRows = errors.size();
    summary.errors = errors;
    summary.existingKeysNotInCsv =
        Math.max(0, existingLiteralsBefore - existingLiteralsMatchedBefore);
    log.info(
        "CSV import completed - language: {}, rows: {}, created: {}L/{}T, updated: {}, emptied: {}, unchanged: {}, failed: {}",
        targetLanguage,
        summary.totalRows,
        summary.createdLiterals,
        summary.createdTranslations,
        summary.updatedTranslations,
        summary.emptiedTranslations,
        summary.unchangedRows,
        summary.failedRows);
    if (summary.failedRows > 0) {
      log.warn(
          "CSV import - {} rows were skipped due to validation/persistence errors",
          summary.failedRows);
    }
    return summary.toResponse();
  }

  private CsvInspection readAndInspectRows(MultipartFile file) {
    try (BOMInputStream bomInputStream =
            BOMInputStream.builder().setInputStream(file.getInputStream()).get();
        InputStreamReader inputStreamReader =
            new InputStreamReader(bomInputStream, StandardCharsets.UTF_8);
        CSVReader reader = createReader(inputStreamReader)) {
      String[] header = reader.readNext();
      validateHeader(header);

      final CsvInspection inspection = new CsvInspection();
      String[] line;
      long rowNumber = 2;
      while ((line = reader.readNext()) != null) {
        inspectRow(inspection, rowNumber++, line);
      }
      return inspection;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV file", e);
    }
  }

  private void validateHeader(String[] header) {
    if (header == null || header.length != HEADER.length) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV header");
    }
    if (!HEADER[0].equals(stripBom(header[0]))
        || !HEADER[1].equals(header[1])
        || !HEADER[2].equals(header[2])) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV header");
    }
  }

  private void inspectRow(CsvInspection inspection, long rowNumber, String[] row) {
    inspection.totalRows++;
    if (row.length != HEADER.length) {
      addInspectionError(inspection, rowNumber, null, null, "Invalid CSV row");
      return;
    }

    final String sourceLanguage = normalizeRequiredCode(getCell(row, 0));
    final String literal = normalizeLiteral(getCell(row, 1));
    final String translation = normalizeLiteral(getCell(row, 2));

    if (!StringUtils.hasText(sourceLanguage)) {
      addInspectionError(
          inspection,
          rowNumber,
          null,
          literal,
          "entity.literalTranslation.error.source_language_required");
      return;
    }
    if (!StringUtils.hasText(literal)) {
      addInspectionError(
          inspection,
          rowNumber,
          sourceLanguage,
          null,
          "entity.literalTranslation.error.literal_required");
      return;
    }
    if (literal.length() > LONG_DESCRIPTION) {
      addInspectionError(
          inspection,
          rowNumber,
          sourceLanguage,
          literal,
          "entity.literalTranslation.error.literal_too_long");
      return;
    }
    if (translation != null && translation.length() > LONG_DESCRIPTION) {
      addInspectionError(
          inspection,
          rowNumber,
          sourceLanguage,
          literal,
          "entity.literalTranslation.error.translation_too_long");
      return;
    }
    if (!inspection.seenLiterals.add(literal)) {
      addInspectionError(
          inspection,
          rowNumber,
          sourceLanguage,
          literal,
          "entity.literalTranslation.error.duplicate_literal");
      return;
    }

    inspection.validRows.add(
        new ValidatedCsvRow(rowNumber, sourceLanguage, literal, translation, null));
    inspection.sourceLanguages.add(sourceLanguage);
    inspection.literals.add(literal);
  }

  private void addInspectionError(
      CsvInspection inspection,
      long rowNumber,
      String sourceLanguage,
      String literal,
      String message) {
    inspection.errors.add(
        new LiteralTranslationCsvImportErrorDto(rowNumber, sourceLanguage, literal, message));
  }

  private RowOutcome processRow(
      ValidatedCsvRow row,
      Language targetLanguage,
      Map<String, ExistingLiteral> existingByLiteral,
      Map<Integer, String> existingTargetValues) {
    ExistingLiteral existing = existingByLiteral.get(row.literal());
    boolean createdLiteral = existing == null;
    ExistingLiteral literal = createdLiteral ? createLiteral(row) : existing;
    LiteralTranslation literalEntity =
        literalTranslationRepository.findById(literal.id()).orElseThrow();

    syncSourceTranslation(literalEntity, targetLanguage.getId());
    return processTargetTranslation(
        literal,
        literalEntity,
        targetLanguage,
        existingTargetValues.get(literal.id()),
        row.translation(),
        createdLiteral);
  }

  private ExistingLiteral createLiteral(ValidatedCsvRow row) {
    Language sourceLanguage =
        languageRepository
            .findById(row.sourceLanguageId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language not found"));
    LiteralTranslation saved =
        literalTranslationRepository.saveAndFlush(
            LiteralTranslation.builder()
                .literal(row.literal())
                .sourceLanguage(sourceLanguage)
                .build());
    return toExisting(saved);
  }

  private RowOutcome processTargetTranslation(
      ExistingLiteral literal,
      LiteralTranslation literalEntity,
      Language targetLanguage,
      String currentValue,
      String translation,
      boolean createdLiteral) {
    if (!StringUtils.hasText(translation)) {
      if (currentValue != null) {
        deleteTranslation(literalEntity, targetLanguage.getShortname());
        return new RowOutcome(literal, null, RowAction.EMPTIED_TRANSLATION, createdLiteral, true);
      }
      return new RowOutcome(literal, null, RowAction.UNCHANGED, createdLiteral, true);
    }

    if (currentValue == null) {
      upsertTranslation(literalEntity, targetLanguage, translation);
      return new RowOutcome(
          literal, translation, RowAction.CREATED_TRANSLATION, createdLiteral, false);
    }

    if (currentValue.equals(translation)) {
      return new RowOutcome(literal, currentValue, RowAction.UNCHANGED, createdLiteral, false);
    }

    upsertTranslation(literalEntity, targetLanguage, translation);
    return new RowOutcome(
        literal, translation, RowAction.UPDATED_TRANSLATION, createdLiteral, false);
  }

  private void applyOutcome(
      Summary summary,
      Map<String, ExistingLiteral> existingByLiteral,
      Map<Integer, String> existingTargetValues,
      RowOutcome outcome) {
    existingByLiteral.put(outcome.literal().literal(), outcome.literal());
    summary.apply(outcome);

    if (outcome.finalTranslation() == null) {
      existingTargetValues.remove(outcome.literal().id());
    } else {
      existingTargetValues.put(outcome.literal().id(), outcome.finalTranslation());
    }
  }

  private LiteralTranslationCsvImportErrorDto toImportError(
      long rowNumber, String sourceLanguage, String literal, RuntimeException exception) {
    String message =
        StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : "Import error";
    return new LiteralTranslationCsvImportErrorDto(rowNumber, sourceLanguage, literal, message);
  }

  private Map<String, Integer> resolveLanguageIds(Collection<String> shortnames) {
    if (shortnames.isEmpty()) {
      return Map.of();
    }
    return languageRepository.findByShortnameIn(shortnames).stream()
        .collect(
            Collectors.toMap(
                Language::getShortname, Language::getId, (a, b) -> a, LinkedHashMap::new));
  }

  private Set<String> sourceLanguagesOf(List<ValidatedCsvRow> rows) {
    return rows.stream()
        .map(ValidatedCsvRow::sourceLanguage)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<String> literalsOf(List<ValidatedCsvRow> rows) {
    return rows.stream()
        .map(ValidatedCsvRow::literal)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Map<String, ExistingLiteral> loadExistingLiterals(Set<String> literals) {
    if (literals.isEmpty()) {
      return new LinkedHashMap<>();
    }
    Map<String, ExistingLiteral> literalsByName = new LinkedHashMap<>();
    for (LiteralTranslation lt : literalTranslationRepository.findByLiteralIn(literals)) {
      ExistingLiteral existing = toExisting(lt);
      literalsByName.put(existing.literal(), existing);
    }
    return literalsByName;
  }

  private Map<Integer, String> loadExistingTargetValues(
      Integer targetLanguageId, Collection<ExistingLiteral> literals) {
    if (literals.isEmpty()) {
      return new LinkedHashMap<>();
    }
    List<Integer> literalIds = literals.stream().map(ExistingLiteral::id).toList();
    Map<Integer, String> values = new LinkedHashMap<>();
    for (LiteralTranslationValue value :
        literalTranslationValueRepository.findByLanguageIdAndLiteralTranslationIdIn(
            targetLanguageId, literalIds)) {
      values.put(value.getLiteralTranslation().getId(), value.getValue());
    }
    return values;
  }

  private void syncSourceTranslation(LiteralTranslation literalEntity, Integer targetLanguageId) {
    Language sourceLanguage = literalEntity.getSourceLanguage();
    if (sourceLanguage.getId().equals(targetLanguageId)) {
      return;
    }
    upsertTranslation(literalEntity, sourceLanguage, literalEntity.getLiteral());
  }

  private void upsertTranslation(
      LiteralTranslation literalEntity, Language language, String value) {
    LiteralTranslationValue entity =
        literalTranslationValueRepository
            .findByLiteralTranslationIdAndLanguageShortname(
                literalEntity.getId(), language.getShortname())
            .orElseGet(LiteralTranslationValue::new);
    entity.setLiteralTranslation(literalEntity);
    entity.setLanguage(language);
    entity.setValue(value);
    literalTranslationValueRepository.saveAndFlush(entity);
  }

  private void deleteTranslation(LiteralTranslation literalEntity, String languageShortname) {
    literalTranslationValueRepository
        .findByLiteralTranslationIdAndLanguageShortname(literalEntity.getId(), languageShortname)
        .ifPresent(literalTranslationValueRepository::delete);
  }

  private String getCell(String[] row, int index) {
    return row != null && row.length > index ? row[index] : null;
  }

  private List<ExportRow> loadExportRows(Integer targetLanguageId, List<Long> literalIds) {
    List<LiteralTranslation> literals;
    if (literalIds != null && !literalIds.isEmpty()) {
      List<Integer> ids = literalIds.stream().map(Long::intValue).toList();
      literals = literalTranslationRepository.findByIdInWithSourceLanguageOrderByLiteral(ids);
    } else {
      literals = literalTranslationRepository.findAllWithSourceLanguageOrderByLiteral();
    }
    if (literals.isEmpty()) {
      return List.of();
    }
    List<Integer> ids = literals.stream().map(LiteralTranslation::getId).toList();
    Map<Integer, String> translations = new HashMap<>();
    for (LiteralTranslationValue value :
        literalTranslationValueRepository.findByLanguageIdAndLiteralTranslationIdIn(
            targetLanguageId, ids)) {
      translations.put(value.getLiteralTranslation().getId(), value.getValue());
    }
    return literals.stream()
        .map(
            lt ->
                new ExportRow(
                    lt.getSourceLanguage().getShortname(),
                    lt.getLiteral(),
                    translations.getOrDefault(lt.getId(), "")))
        .toList();
  }

  private static ExistingLiteral toExisting(LiteralTranslation lt) {
    return new ExistingLiteral(
        lt.getId(),
        lt.getLiteral(),
        lt.getSourceLanguage().getShortname(),
        lt.getSourceLanguage().getId());
  }

  private record ValidatedCsvRow(
      long rowNumber,
      String sourceLanguage,
      String literal,
      String translation,
      Integer sourceLanguageId) {

    private ValidatedCsvRow withSourceLanguageId(Integer sourceLanguageId) {
      return new ValidatedCsvRow(rowNumber, sourceLanguage, literal, translation, sourceLanguageId);
    }
  }

  private record ExportRow(String sourceLanguage, String literal, String translation) {}

  private record ExistingLiteral(
      Integer id, String literal, String sourceLanguage, Integer sourceLanguageId) {}

  private enum RowAction {
    CREATED_TRANSLATION,
    UPDATED_TRANSLATION,
    EMPTIED_TRANSLATION,
    UNCHANGED
  }

  private record RowOutcome(
      ExistingLiteral literal,
      String finalTranslation,
      RowAction action,
      boolean createdLiteral,
      boolean emptyValueRow) {}

  private static final class CsvInspection {
    private final List<ValidatedCsvRow> validRows = new ArrayList<>();
    private final Set<String> seenLiterals = new LinkedHashSet<>();
    private final Set<String> sourceLanguages = new LinkedHashSet<>();
    private final Set<String> literals = new LinkedHashSet<>();
    private final List<LiteralTranslationCsvImportErrorDto> errors = new ArrayList<>();
    private long totalRows;
  }

  private static final class Summary {
    private final String targetLanguage;
    private final List<String> sourceLanguages;
    private long totalRows;
    private long createdLiterals;
    private long createdTranslations;
    private long updatedTranslations;
    private long emptiedTranslations;
    private long unchangedRows;
    private long existingKeysNotInCsv;
    private long emptyValueRows;
    private long failedRows;
    private List<LiteralTranslationCsvImportErrorDto> errors = List.of();

    private Summary(String targetLanguage, Collection<String> sourceLanguages, long totalRows) {
      this.targetLanguage = targetLanguage;
      this.sourceLanguages = new ArrayList<>(sourceLanguages);
      this.totalRows = totalRows;
    }

    private void apply(RowOutcome outcome) {
      if (outcome.createdLiteral()) {
        createdLiterals++;
      }
      if (outcome.emptyValueRow()) {
        emptyValueRows++;
      }
      switch (outcome.action()) {
        case CREATED_TRANSLATION -> createdTranslations++;
        case UPDATED_TRANSLATION -> updatedTranslations++;
        case EMPTIED_TRANSLATION -> emptiedTranslations++;
        case UNCHANGED -> unchangedRows++;
      }
    }

    private LiteralTranslationCsvImportResponseDto toResponse() {
      return LiteralTranslationCsvImportResponseDto.builder()
          .targetLanguage(targetLanguage)
          .totalRows(totalRows)
          .createdLiterals(createdLiterals)
          .createdTranslations(createdTranslations)
          .updatedTranslations(updatedTranslations)
          .emptiedTranslations(emptiedTranslations)
          .unchangedRows(unchangedRows)
          .existingKeysNotInCsv(existingKeysNotInCsv)
          .emptyValueRows(emptyValueRows)
          .failedRows(failedRows)
          .sourceLanguages(sourceLanguages)
          .errors(errors)
          .build();
    }
  }
}
