package org.sitmun.administration.service;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.administration.dto.DefaultLanguageChangePreview;
import org.sitmun.administration.dto.DefaultLanguageChangeRequest;
import org.sitmun.administration.dto.DefaultLanguageChangeResult;
import org.sitmun.administration.dto.MissingTranslationDto;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Translation;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Service for managing database default language changes with lossless translation migration. */
@Service
@Slf4j
public class DefaultLanguageChangeService {

  private final LanguageRepository languageRepository;
  private final TranslationRepository translationRepository;
  private final ConfigurationParameterRepository configurationParameterRepository;
  private final LiteralTranslationRepository literalTranslationRepository;
  private final LiteralTranslationValueRepository literalTranslationValueRepository;
  private final JdbcTemplate jdbcTemplate;

  /** Catalog of translatable fields across all entities with @I18n annotations. */
  private static final List<TranslatableField> TRANSLATABLE_CATALOG =
      List.of(
          // Language
          new TranslatableField("Language", "STM_LANGUAGE", "LAN_ID", "name", "LAN_NAME"),
          // Application
          new TranslatableField("Application", "STM_APP", "APP_ID", "name", "APP_NAME"),
          new TranslatableField(
              "Application", "STM_APP", "APP_ID", "description", "APP_DESCRIPTION"),
          new TranslatableField("Application", "STM_APP", "APP_ID", "title", "APP_TITLE"),
          new TranslatableField(
              "Application",
              "STM_APP",
              "APP_ID",
              "maintenanceInformation",
              "APP_MAINTENANCE_INFORMATION"),
          // Service
          new TranslatableField("Service", "STM_SERVICE", "SER_ID", "name", "SER_NAME"),
          new TranslatableField("Service", "STM_SERVICE", "SER_ID", "description", "SER_ABSTRACT"),
          // Territory
          new TranslatableField("Territory", "STM_TERRITORY", "TER_ID", "name", "TER_NAME"),
          new TranslatableField(
              "Territory", "STM_TERRITORY", "TER_ID", "description", "TER_DESCRIPTION"),
          // TerritoryType
          new TranslatableField("TerritoryType", "STM_TER_TYP", "TET_ID", "name", "TET_NAME"),
          // Background
          new TranslatableField("Background", "STM_BACKGRD", "BAC_ID", "name", "BAC_NAME"),
          new TranslatableField("Background", "STM_BACKGRD", "BAC_ID", "description", "BAC_DESC"),
          // Cartography
          new TranslatableField("Cartography", "STM_GEOINFO", "GEO_ID", "name", "GEO_NAME"),
          new TranslatableField(
              "Cartography", "STM_GEOINFO", "GEO_ID", "description", "GEO_ABSTRACT"),
          // Tree
          new TranslatableField("Tree", "STM_TREE", "TRE_ID", "name", "TRE_NAME"),
          new TranslatableField("Tree", "STM_TREE", "TRE_ID", "description", "TRE_ABSTRACT"),
          // TreeNode
          new TranslatableField("TreeNode", "STM_TREE_NOD", "TNO_ID", "name", "TNO_NAME"),
          new TranslatableField(
              "TreeNode", "STM_TREE_NOD", "TNO_ID", "description", "TNO_ABSTRACT"),
          // CodeListValue
          new TranslatableField(
              "CodeListValue", "STM_CODELIST", "COD_ID", "description", "COD_DESCRIPTION"),
          // Task
          new TranslatableField("Task", "STM_TASK", "TAS_ID", "name", "TAS_NAME"),
          // TaskGroup
          new TranslatableField("TaskGroup", "STM_GRP_TSK", "GTS_ID", "name", "GTS_NAME"),
          // TaskType
          new TranslatableField("TaskType", "STM_TSK_TYP", "TTY_ID", "title", "TTY_TITLE"));

  public DefaultLanguageChangeService(
      LanguageRepository languageRepository,
      TranslationRepository translationRepository,
      ConfigurationParameterRepository configurationParameterRepository,
      LiteralTranslationRepository literalTranslationRepository,
      LiteralTranslationValueRepository literalTranslationValueRepository,
      DataSource dataSource) {
    this.languageRepository = languageRepository;
    this.translationRepository = translationRepository;
    this.configurationParameterRepository = configurationParameterRepository;
    this.literalTranslationRepository = literalTranslationRepository;
    this.literalTranslationValueRepository = literalTranslationValueRepository;
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * Preview a default language change without modifying data.
   *
   * @param from Source language shortname
   * @param to Target language shortname
   * @return Preview with affected counts and missing translations
   */
  public DefaultLanguageChangePreview preview(String from, String to) {
    validateLanguages(from, to);

    Language sourceLang =
        languageRepository
            .findByShortname(from)
            .orElseThrow(() -> new IllegalArgumentException("Source language not found: " + from));
    Language targetLang =
        languageRepository
            .findByShortname(to)
            .orElseThrow(() -> new IllegalArgumentException("Target language not found: " + to));
    List<MissingTranslationDto> missingTranslations = checkMissingTranslations(targetLang);
    int affectedValues = countAffectedValues();
    int restoredValues = countRestorableValues(targetLang);
    int literalContinuitySeeds = countLiteralContinuitySeeds(sourceLang, targetLang);

    return new DefaultLanguageChangePreview(
        from,
        to,
        affectedValues,
        affectedValues,
        restoredValues,
        missingTranslations.size(),
        missingTranslations,
        literalContinuitySeeds);
  }

  /**
   * Apply a default language change with lossless translation migration.
   *
   * @param request Change request with source, target, and continuation flag
   * @return Result with backup, restore, and preserve counts
   */
  @Transactional
  public DefaultLanguageChangeResult apply(DefaultLanguageChangeRequest request) {
    validateLanguages(request.from(), request.to());

    if (request.from().equals(request.to())) {
      throw new IllegalArgumentException("Source and target languages must be different");
    }

    Language sourceLang =
        languageRepository
            .findByShortname(request.from())
            .orElseThrow(
                () -> new IllegalArgumentException("Source language not found: " + request.from()));
    Language targetLang =
        languageRepository
            .findByShortname(request.to())
            .orElseThrow(
                () -> new IllegalArgumentException("Target language not found: " + request.to()));

    // Lock the configuration parameter
    ConfigurationParameter defaultLangParam =
        configurationParameterRepository
            .findByNameForUpdate("language.default")
            .orElseThrow(
                () -> new IllegalStateException("language.default configuration not found"));

    // Check for missing translations
    List<MissingTranslationDto> missingTranslations = checkMissingTranslations(targetLang);

    if (!missingTranslations.isEmpty() && !request.continueOnMissingTranslations()) {
      throw new IllegalStateException(
          "Cannot change default language: "
              + missingTranslations.size()
              + " missing translations found. Set continueOnMissingTranslations=true to preserve current values.");
    }

    // Backup current main-table values to source language translations
    int backupUpserts = backupCurrentValues(sourceLang);

    // Flush to ensure backups are persisted before updating main tables
    translationRepository.flush();

    // Restore target language translations to main tables
    int restoredValues = restoreTargetValues(targetLang, !missingTranslations.isEmpty());

    int literalContinuitySeeds = seedLiteralContinuityValues(sourceLang, targetLang);

    // Update configuration
    defaultLangParam.setValue(request.to());
    configurationParameterRepository.save(defaultLangParam);

    log.info(
        "Changed default language from {} to {}: backup={}, restored={}, preserved={}, literalSeeds={}",
        request.from(),
        request.to(),
        backupUpserts,
        restoredValues,
        missingTranslations.size(),
        literalContinuitySeeds);

    return new DefaultLanguageChangeResult(
        request.from(),
        request.to(),
        backupUpserts,
        restoredValues,
        missingTranslations.size(),
        missingTranslations,
        literalContinuitySeeds);
  }

  private void validateLanguages(String from, String to) {
    if (languageRepository.findByShortname(from).isEmpty()) {
      throw new IllegalArgumentException("Source language not found: " + from);
    }
    Language target =
        languageRepository
            .findByShortname(to)
            .orElseThrow(() -> new IllegalArgumentException("Target language not found: " + to));
    if (Boolean.FALSE.equals(target.getEnabled())) {
      throw new IllegalArgumentException("Target language is disabled: " + to);
    }
  }

  private List<MissingTranslationDto> checkMissingTranslations(Language targetLang) {
    List<MissingTranslationDto> missing = new ArrayList<>();

    for (TranslatableField field : TRANSLATABLE_CATALOG) {
      String sql =
          String.format(
              """
              SELECT t.%s, t.%s
              FROM %s t
              WHERE NOT EXISTS (
                SELECT 1 FROM STM_TRANSLATION tr
                WHERE tr.TRA_ELEID = t.%s
                AND tr.TRA_COLUMN = ?
                AND tr.TRA_LANID = ?
              )
              """,
              field.idColumn(), field.column(), field.table(), field.idColumn());

      jdbcTemplate.query(
          sql,
          (ResultSetExtractor<Void>)
              rs -> {
                while (rs.next()) {
                  missing.add(
                      new MissingTranslationDto(
                          field.entity(),
                          rs.getInt(field.idColumn()),
                          field.translationColumn(),
                          rs.getString(field.column())));
                }
                return null;
              },
          field.translationColumn(),
          targetLang.getId());
    }

    return missing;
  }

  private int countAffectedValues() {
    int count = 0;

    for (TranslatableField field : TRANSLATABLE_CATALOG) {
      count +=
          jdbcTemplate.queryForObject(
              String.format(
                  "SELECT COUNT(*) FROM %s WHERE %s IS NOT NULL", field.table(), field.column()),
              Integer.class);
    }

    return count;
  }

  private int countRestorableValues(Language targetLang) {
    int count = 0;

    for (TranslatableField field : TRANSLATABLE_CATALOG) {
      String sql =
          String.format(
              """
              SELECT COUNT(*)
              FROM %s t
              WHERE EXISTS (
                SELECT 1 FROM STM_TRANSLATION tr
                WHERE tr.TRA_ELEID = t.%s
                AND tr.TRA_COLUMN = ?
                AND tr.TRA_LANID = ?
              )
              """,
              field.table(), field.idColumn());

      count +=
          jdbcTemplate.queryForObject(
              sql, Integer.class, field.translationColumn(), targetLang.getId());
    }

    return count;
  }

  private int backupCurrentValues(Language sourceLang) {
    int count = 0;

    for (TranslatableField field : TRANSLATABLE_CATALOG) {
      // Upsert current main-table values into source language translations
      String sql =
          String.format(
              """
              SELECT t.%s, t.%s
              FROM %s t
              """,
              field.idColumn(), field.column(), field.table());

      jdbcTemplate.query(
          sql,
          (ResultSetExtractor<Void>)
              rs -> {
                while (rs.next()) {
                  Integer elementId = rs.getInt(field.idColumn());
                  String currentValue = rs.getString(field.column());

                  if (currentValue != null && !currentValue.isBlank()) {
                    upsertTranslation(
                        elementId, field.translationColumn(), sourceLang, currentValue);
                  }
                }
                return null;
              });

      count +=
          jdbcTemplate.queryForObject(
              String.format("SELECT COUNT(*) FROM %s", field.table()), Integer.class);
    }

    return count;
  }

  private int restoreTargetValues(Language targetLang, boolean preserveMissing) {
    int count = 0;

    for (TranslatableField field : TRANSLATABLE_CATALOG) {
      String selectTranslations =
          """
          SELECT tr.TRA_ELEID, tr.TRA_NAME
          FROM STM_TRANSLATION tr
          WHERE tr.TRA_COLUMN = ?
          AND tr.TRA_LANID = ?
          """;

      String updateMainValue =
          String.format(
              """
              UPDATE %s
              SET %s = ?
              WHERE %s = ?
              """,
              field.table(), field.column(), field.idColumn());

      count +=
          jdbcTemplate.query(
              selectTranslations,
              (ResultSetExtractor<Integer>)
                  rs -> {
                    int updated = 0;
                    while (rs.next()) {
                      updated +=
                          jdbcTemplate.update(
                              updateMainValue, rs.getString("TRA_NAME"), rs.getInt("TRA_ELEID"));
                    }
                    return updated;
                  },
              field.translationColumn(),
              targetLang.getId());
    }

    return count;
  }

  private void upsertTranslation(
      Integer elementId, String column, Language language, String value) {
    // Check if translation exists
    List<Translation> existing =
        translationRepository.findByElementAndColumnStartingWith(elementId, column);

    Translation translation =
        existing.stream()
            .filter(t -> t.getLanguage().getId().equals(language.getId()))
            .findFirst()
            .orElse(
                Translation.builder().element(elementId).column(column).language(language).build());

    translation.setTranslation(value);
    translationRepository.save(translation);
  }

  private int countLiteralContinuitySeeds(Language sourceLang, Language targetLang) {
    int count = 0;
    for (LiteralTranslation literal :
        literalTranslationRepository.findAllWithSourceLanguageOrderByLiteral()) {
      if (resolveContinuitySeedValue(literal, sourceLang, targetLang) != null) {
        count++;
      }
    }
    return count;
  }

  private int seedLiteralContinuityValues(Language sourceLang, Language targetLang) {
    int seeded = 0;
    for (LiteralTranslation literal :
        literalTranslationRepository.findAllWithSourceLanguageOrderByLiteral()) {
      String value = resolveContinuitySeedValue(literal, sourceLang, targetLang);
      if (value == null) {
        continue;
      }
      LiteralTranslationValue row = new LiteralTranslationValue();
      row.setLiteralTranslation(literal);
      row.setLanguage(targetLang);
      row.setValue(value);
      literalTranslationValueRepository.save(row);
      seeded++;
    }
    return seeded;
  }

  /**
   * Returns the continuity value to seed for {@code targetLang}, or null when target already has a
   * value or no previous value can be resolved.
   */
  private String resolveContinuitySeedValue(
      LiteralTranslation literal, Language sourceLang, Language targetLang) {
    if (literalTranslationValueRepository
        .findByLiteralTranslationIdAndLanguageShortname(literal.getId(), targetLang.getShortname())
        .isPresent()) {
      return null;
    }
    return literalTranslationValueRepository
        .findValueByLiteralIdAndLanguage(literal.getId(), sourceLang.getShortname())
        .filter(StringUtils::hasText)
        .or(
            () -> {
              Language sourceLanguage = literal.getSourceLanguage();
              if (sourceLanguage == null || !StringUtils.hasText(sourceLanguage.getShortname())) {
                return java.util.Optional.empty();
              }
              return literalTranslationValueRepository.findValueByLiteralIdAndLanguage(
                  literal.getId(), sourceLanguage.getShortname());
            })
        .filter(StringUtils::hasText)
        .or(() -> java.util.Optional.ofNullable(literal.getLiteral()).filter(StringUtils::hasText))
        .orElse(null);
  }
}
