package org.sitmun.administration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.dto.DefaultLanguageChangePreview;
import org.sitmun.administration.dto.DefaultLanguageChangeRequest;
import org.sitmun.administration.dto.DefaultLanguageChangeResult;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Translation;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.sitmun.test.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DefaultLanguageChangeServiceIntegrationTest extends BaseTest {

  @Autowired private DefaultLanguageChangeService service;

  @Autowired private LanguageRepository languageRepository;

  @Autowired private TranslationRepository translationRepository;

  @Autowired private ConfigurationParameterRepository configurationParameterRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private LiteralTranslationRepository literalTranslationRepository;

  @Autowired private LiteralTranslationValueRepository literalTranslationValueRepository;

  @Autowired private TaskRepository taskRepository;

  @Autowired private ServiceRepository serviceRepository;

  @Autowired private EntityManager entityManager;

  private Language english;
  private Language catalan;
  private ConfigurationParameter defaultLangParam;

  @BeforeEach
  void setUp() {
    // Use existing test languages from seed data
    english = languageRepository.findByShortname("en").orElseThrow();
    catalan = languageRepository.findByShortname("ca").orElseThrow();

    // Clean up any existing translations for Language.name to ensure known state
    translationRepository
        .findByElementAndColumnStartingWith(english.getId(), "Language.name")
        .forEach(translationRepository::delete);
    translationRepository
        .findByElementAndColumnStartingWith(catalan.getId(), "Language.name")
        .forEach(translationRepository::delete);

    // Ensure default language configuration exists
    defaultLangParam =
        configurationParameterRepository
            .findByName("language.default")
            .orElseGet(
                () ->
                    configurationParameterRepository.save(
                        ConfigurationParameter.builder()
                            .name("language.default")
                            .value("en")
                            .build()));

    // Reset to English if needed
    if (!"en".equals(defaultLangParam.getValue())) {
      defaultLangParam.setValue("en");
      configurationParameterRepository.save(defaultLangParam);
    }
  }

  @Test
  void shouldMigrateLanguageNamesLosslessly() {
    // Given: English is default, Catalan translation exists
    translationRepository.save(
        Translation.builder()
            .element(english.getId())
            .column("Language.name")
            .language(catalan)
            .translation("Anglès")
            .build());

    // When: Change default from English to Catalan (continue on missing to handle other entities)
    DefaultLanguageChangeRequest request = new DefaultLanguageChangeRequest("en", "ca", true);
    DefaultLanguageChangeResult result = service.apply(request);

    // Then: Operation successful
    assertThat(result.previousDefault()).isEqualTo("en");
    assertThat(result.currentDefault()).isEqualTo("ca");
    assertThat(result.backupUpserts()).isGreaterThan(0);
    assertThat(result.restoredValues()).isGreaterThan(0);

    // Verify configuration updated
    ConfigurationParameter updated =
        configurationParameterRepository.findByName("language.default").orElseThrow();
    assertThat(updated.getValue()).isEqualTo("ca");

    // Verify English value backed up as translation
    assertThat(
            translationRepository.findByElementAndColumnStartingWith(
                english.getId(), "Language.name"))
        .anySatisfy(
            t -> {
              if (t.getLanguage().getId().equals(english.getId())) {
                assertThat(t.getTranslation()).isEqualTo("English");
              }
            });
  }

  @Test
  void shouldBlockWhenTargetTranslationsMissingAndContinueFalse() {
    // Given: No Catalan translation for English language name
    // When/Then: Migration blocked
    DefaultLanguageChangeRequest request = new DefaultLanguageChangeRequest("en", "ca", false);

    assertThatThrownBy(() -> service.apply(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missing translations");

    // Verify configuration unchanged
    ConfigurationParameter unchanged =
        configurationParameterRepository.findByName("language.default").orElseThrow();
    assertThat(unchanged.getValue()).isEqualTo("en");
  }

  @Test
  void shouldPreserveCurrentValuesWhenTargetMissingAndContinueTrue() {
    // Given: No Catalan translation for English language name
    // Read the raw database value before migration
    String originalEnglishName =
        jdbcTemplate.queryForObject(
            "SELECT LAN_NAME FROM STM_LANGUAGE WHERE LAN_ID = ?", String.class, english.getId());

    // When: Change with continue flag
    DefaultLanguageChangeRequest request = new DefaultLanguageChangeRequest("en", "ca", true);
    DefaultLanguageChangeResult result = service.apply(request);

    // Then: Current values preserved
    assertThat(result.currentDefault()).isEqualTo("ca");
    assertThat(result.preservedValues()).isGreaterThan(0);
    assertThat(result.preservedMissing()).isNotEmpty();

    // Verify main table value unchanged (read raw database value to avoid i18n overlay)
    String currentEnglishName =
        jdbcTemplate.queryForObject(
            "SELECT LAN_NAME FROM STM_LANGUAGE WHERE LAN_ID = ?", String.class, english.getId());
    assertThat(currentEnglishName).isEqualTo(originalEnglishName);
  }

  @Test
  void shouldNotDeleteExistingTranslationsDuringMigration() {
    // Given: Existing French translation
    Language french =
        languageRepository
            .findByShortname("fr")
            .orElseGet(
                () ->
                    languageRepository.save(
                        Language.builder().shortname("fr").name("French").build()));

    Translation frenchTranslation =
        translationRepository.save(
            Translation.builder()
                .element(english.getId())
                .column("Language.name")
                .language(french)
                .translation("Anglais")
                .build());

    // When: Migrate from English to Catalan
    translationRepository.save(
        Translation.builder()
            .element(english.getId())
            .column("Language.name")
            .language(catalan)
            .translation("Anglès")
            .build());

    DefaultLanguageChangeRequest request = new DefaultLanguageChangeRequest("en", "ca", true);
    service.apply(request);

    // Then: French translation still exists
    Translation frenchStillExists =
        translationRepository.findById(frenchTranslation.getId()).orElseThrow();
    assertThat(frenchStillExists.getTranslation()).isEqualTo("Anglais");
  }

  @Test
  void shouldSeedLiteralContinuityValueForNewDefaultWithoutChangingSourceLanguage() {
    String key = "continuity-" + System.nanoTime();
    LiteralTranslation literal =
        literalTranslationRepository.save(
            LiteralTranslation.builder().literal(key).sourceLanguage(english).build());
    LiteralTranslationValue enValue = new LiteralTranslationValue();
    enValue.setLiteralTranslation(literal);
    enValue.setLanguage(english);
    enValue.setValue("Hello A");
    literalTranslationValueRepository.save(enValue);

    translationRepository.save(
        Translation.builder()
            .element(english.getId())
            .column("Language.name")
            .language(catalan)
            .translation("Anglès")
            .build());

    DefaultLanguageChangePreview preview = service.preview("en", "ca");
    assertThat(preview.literalContinuitySeeds()).isGreaterThanOrEqualTo(1);

    DefaultLanguageChangeResult result =
        service.apply(new DefaultLanguageChangeRequest("en", "ca", true));
    assertThat(result.literalContinuitySeeds()).isGreaterThanOrEqualTo(1);

    assertThat(
            literalTranslationValueRepository.findValueByLiteralIdAndLanguage(
                literal.getId(), "ca"))
        .contains("Hello A");
    assertThat(
            literalTranslationRepository
                .findById(literal.getId())
                .orElseThrow()
                .getSourceLanguage()
                .getShortname())
        .isEqualTo("en");
  }

  @Test
  void shouldMigrateTaskNameAndServiceNameFromCatalog() {
    long stamp = System.nanoTime();
    String taskEn = "Task EN " + stamp;
    String serviceEn = "Svc EN " + stamp;
    Task task = taskRepository.save(Task.builder().name(taskEn).build());
    Service svc =
        serviceRepository.save(
            Service.builder()
                .name(serviceEn)
                .serviceURL("http://localhost/api/services/dlc-" + stamp)
                .type("WMS")
                .blocked(false)
                .build());

    translationRepository.save(
        Translation.builder()
            .element(task.getId())
            .column("Task.name")
            .language(catalan)
            .translation("Task CA")
            .build());
    translationRepository.save(
        Translation.builder()
            .element(svc.getId())
            .column("Service.name")
            .language(catalan)
            .translation("Service CA")
            .build());
    translationRepository.save(
        Translation.builder()
            .element(english.getId())
            .column("Language.name")
            .language(catalan)
            .translation("Anglès")
            .build());
    entityManager.flush();

    service.apply(new DefaultLanguageChangeRequest("en", "ca", true));
    entityManager.flush();
    entityManager.clear();

    String taskNameAfter =
        jdbcTemplate.queryForObject(
            "SELECT TAS_NAME FROM STM_TASK WHERE TAS_ID = ?", String.class, task.getId());
    String serviceNameAfter =
        jdbcTemplate.queryForObject(
            "SELECT SER_NAME FROM STM_SERVICE WHERE SER_ID = ?", String.class, svc.getId());
    assertThat(taskNameAfter).isEqualTo("Task CA");
    assertThat(serviceNameAfter).isEqualTo("Service CA");

    assertThat(translationRepository.findByElementAndColumnStartingWith(task.getId(), "Task.name"))
        .anySatisfy(
            t -> {
              if (t.getLanguage().getId().equals(english.getId())) {
                assertThat(t.getTranslation()).isEqualTo(taskEn);
              }
            });
    assertThat(
            translationRepository.findByElementAndColumnStartingWith(svc.getId(), "Service.name"))
        .anySatisfy(
            t -> {
              if (t.getLanguage().getId().equals(english.getId())) {
                assertThat(t.getTranslation()).isEqualTo(serviceEn);
              }
            });
  }

  @Test
  void shouldPreviewWithoutModifyingData() {
    // Given
    String originalDefaultValue = defaultLangParam.getValue();

    // When
    DefaultLanguageChangePreview preview = service.preview("en", "ca");

    // Then
    assertThat(preview.currentDefault()).isEqualTo("en");
    assertThat(preview.requestedDefault()).isEqualTo("ca");
    assertThat(preview.affectedValues()).isGreaterThan(0);
    assertThat(preview.backupUpserts()).isEqualTo(preview.affectedValues());
    assertThat(preview.missingTranslations()).isEqualTo(preview.missing().size());
    assertThat(preview.missingTranslations()).isGreaterThan(0);

    // Verify no changes to database
    ConfigurationParameter unchanged =
        configurationParameterRepository.findByName("language.default").orElseThrow();
    assertThat(unchanged.getValue()).isEqualTo(originalDefaultValue);
  }
}
