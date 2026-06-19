package org.sitmun.administration.service.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sitmun.administration.controller.dto.LiteralTranslationListItemDto;
import org.sitmun.administration.controller.dto.LiteralTranslationUpsertRequestDto;
import org.sitmun.infrastructure.persistence.type.i18n.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

class LiteralTranslationCrudServiceTest {

  private LiteralTranslationRepository literalRepository;
  private LiteralTranslationValueRepository valueRepository;
  private LanguageRepository languageRepository;
  private LiteralTranslationCrudService service;

  @BeforeEach
  void setUp() {
    literalRepository = mock(LiteralTranslationRepository.class);
    valueRepository = mock(LiteralTranslationValueRepository.class);
    languageRepository = mock(LanguageRepository.class);
    service =
        new LiteralTranslationCrudService(
            literalRepository, valueRepository, languageRepository, new ObjectMapper());
  }

  @Test
  void createStoresSourceAndTargetTranslationsAndMarksComplete() {
    LiteralTranslationUpsertRequestDto request = request("Hola món!", "Hola mundo!", "es", "ca");

    when(literalRepository.findByLiteral("Hola món!")).thenReturn(Optional.empty());
    when(literalRepository.isCompleteByLiteral("Hola món!")).thenReturn(true);
    when(literalRepository.save(any(LiteralTranslation.class)))
        .thenAnswer(
            invocation -> {
              LiteralTranslation literal = invocation.getArgument(0);
              literal.setId(7);
              return literal;
            });
    stubLanguage("ca", 1, "Catalan");
    stubLanguage("es", 2, "Spanish");
    when(valueRepository.findByLiteralTranslationIdAndLanguageShortname(anyInt(), anyString()))
        .thenReturn(Optional.empty());
    when(valueRepository.save(any(LiteralTranslationValue.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LiteralTranslationListItemDto item = service.create(request);
    Map<String, String> savedValuesByLanguage = captureSavedValuesByLanguage();

    assertThat(item.getId()).isEqualTo(7);
    assertThat(item.getLiteral()).isEqualTo("Hola món!");
    assertThat(item.getTranslation()).isEqualTo("Hola mundo!");
    assertThat(item.getSourceLanguage()).isEqualTo("ca");
    assertThat(item.getComplete()).isTrue();
    assertThat(savedValuesByLanguage)
        .containsEntry("es", "Hola mundo!")
        .containsEntry("ca", "Hola món!");
  }

  @Test
  void createRejectsDuplicateLiteral() {
    LiteralTranslationUpsertRequestDto request = request("Hola món!", null, "es", "ca");

    stubLanguage("ca", 1, "Catalan");
    when(literalRepository.findByLiteral("Hola món!"))
        .thenReturn(Optional.of(LiteralTranslation.builder().id(1).literal("Hola món!").build()));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Literal already exists");
  }

  @Test
  void updateRejectsSourceLanguageChanges() {
    LiteralTranslationUpsertRequestDto request = request("Hola món!", "Hola mundo!", "es", "es");

    when(literalRepository.findById(7))
        .thenReturn(Optional.of(literalTranslation(7, "Hola món!", language(1, "ca", "Catalan"))));
    stubLanguage("es", 2, "Spanish");
    when(literalRepository.findByLiteral("Hola món!"))
        .thenReturn(Optional.of(LiteralTranslation.builder().id(7).literal("Hola món!").build()));

    assertThatThrownBy(() -> service.update(7, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Source language cannot be changed");
  }

  @Test
  void listParsesSimpleBlankFilter() {
    when(literalRepository.findPageByLanguage(anyString(), any(), any()))
        .thenReturn(new PageImpl<>(java.util.List.of()));

    service.list(
        "bg",
        """
        {"translation":{"filterType":"text","type":"blank"}}
        """,
        PageRequest.of(0, 20));

    ArgumentCaptor<LiteralTranslationFilterModel> filterCaptor =
        ArgumentCaptor.forClass(LiteralTranslationFilterModel.class);
    verify(literalRepository).findPageByLanguage(eq("bg"), filterCaptor.capture(), any());

    LiteralTranslationFilterModel.ColumnFilter columnFilter =
        filterCaptor.getValue().columnFilter("translation");
    assertThat(columnFilter).isNotNull();
    assertThat(columnFilter.operator()).isEqualTo("AND");
    assertThat(columnFilter.conditions())
        .containsExactly(new LiteralTranslationFilterModel.Condition("blank", null));
  }

  @Test
  void listParsesAgGridOrConditions() {
    when(literalRepository.findPageByLanguage(anyString(), any(), any())).thenReturn(Page.empty());

    service.list(
        "bg",
        """
        {"translation":{"filterType":"text","operator":"OR","condition1":{"filterType":"text","type":"blank"},"condition2":{"filterType":"text","type":"contains","filter":"sdg"},"conditions":[{"filterType":"text","type":"blank"},{"filterType":"text","type":"contains","filter":"sdg"}]}}
        """,
        PageRequest.of(0, 100));

    ArgumentCaptor<LiteralTranslationFilterModel> filterCaptor =
        ArgumentCaptor.forClass(LiteralTranslationFilterModel.class);
    verify(literalRepository).findPageByLanguage(eq("bg"), filterCaptor.capture(), any());

    LiteralTranslationFilterModel.ColumnFilter columnFilter =
        filterCaptor.getValue().columnFilter("translation");
    assertThat(columnFilter).isNotNull();
    assertThat(columnFilter.operator()).isEqualTo("OR");
    assertThat(columnFilter.conditions())
        .containsExactly(
            new LiteralTranslationFilterModel.Condition("blank", null),
            new LiteralTranslationFilterModel.Condition("contains", "sdg"));
  }

  @Test
  void listParsesAgGridConditionPairsWithoutConditionsArray() {
    when(literalRepository.findPageByLanguage(anyString(), any(), any())).thenReturn(Page.empty());

    service.list(
        "bg",
        """
        {"translation":{"filterType":"text","operator":"OR","condition1":{"filterType":"text","type":"blank"},"condition2":{"filterType":"text","type":"contains","filter":"sdg"}}}
        """,
        PageRequest.of(0, 100));

    ArgumentCaptor<LiteralTranslationFilterModel> filterCaptor =
        ArgumentCaptor.forClass(LiteralTranslationFilterModel.class);
    verify(literalRepository).findPageByLanguage(eq("bg"), filterCaptor.capture(), any());

    LiteralTranslationFilterModel.ColumnFilter columnFilter =
        filterCaptor.getValue().columnFilter("translation");
    assertThat(columnFilter.operator()).isEqualTo("OR");
    assertThat(columnFilter.conditions())
        .containsExactly(
            new LiteralTranslationFilterModel.Condition("blank", null),
            new LiteralTranslationFilterModel.Condition("contains", "sdg"));
  }

  @Test
  void listRejectsInvalidFilterJson() {
    assertThatThrownBy(() -> service.list("bg", "{not-json}", PageRequest.of(0, 20)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid filter payload");
  }

  private LiteralTranslationUpsertRequestDto request(
      String literal, String translation, String language, String sourceLanguage) {
    LiteralTranslationUpsertRequestDto request = new LiteralTranslationUpsertRequestDto();
    request.setLiteral(literal);
    request.setTranslation(translation);
    request.setLanguage(language);
    request.setSourceLanguage(sourceLanguage);
    return request;
  }

  private void stubLanguage(String shortname, int id, String name) {
    when(languageRepository.findByShortname(shortname))
        .thenReturn(Optional.of(language(id, shortname, name)));
  }

  private Language language(int id, String shortname, String name) {
    return Language.builder().id(id).shortname(shortname).name(name).build();
  }

  private LiteralTranslation literalTranslation(int id, String literal, Language sourceLanguage) {
    return LiteralTranslation.builder()
        .id(id)
        .literal(literal)
        .sourceLanguage(sourceLanguage)
        .build();
  }

  private Map<String, String> captureSavedValuesByLanguage() {
    ArgumentCaptor<LiteralTranslationValue> valueCaptor =
        ArgumentCaptor.forClass(LiteralTranslationValue.class);
    verify(valueRepository, times(2)).save(valueCaptor.capture());

    Map<String, String> savedValuesByLanguage = new LinkedHashMap<>();
    for (LiteralTranslationValue value : valueCaptor.getAllValues()) {
      savedValuesByLanguage.put(value.getLanguage().getShortname(), value.getValue());
    }
    return savedValuesByLanguage;
  }
}
