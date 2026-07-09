package org.sitmun.administration.service.i18n;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.LiteralTranslationListItemDto;
import org.sitmun.administration.controller.dto.LiteralTranslationUpsertRequestDto;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LiteralTranslationCrudService {

  public static final String FILTER = "filter";
  private final LiteralTranslationRepository literalTranslationRepository;
  private final LiteralTranslationValueRepository literalTranslationValueRepository;
  private final LanguageRepository languageRepository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public Page<LiteralTranslationListItemDto> list(
      String language, String filter, String searchText, Pageable pageable) {
    String safeLanguage = requireLanguage(language);
    return literalTranslationRepository.findPageByLanguage(
        safeLanguage, parseFilter(filter), searchText, pageable);
  }

  public Double getLanguageCompletionPct(final String shortName) {
    final long totalTranslations = literalTranslationRepository.countTotalTranslations();

    if (totalTranslations == 0) {
      return 0.0;
    }

    final long totalTranslated =
        literalTranslationValueRepository.countByLanguage_Shortname(shortName);
    return (totalTranslated / (double) totalTranslations) * 100;
  }

  @Transactional
  public LiteralTranslationListItemDto create(LiteralTranslationUpsertRequestDto requestDto) {
    String literal = requireLiteral(requestDto.getLiteral());
    String language = requireLanguage(requestDto.getLanguage());
    Language sourceLanguage = requireSourceLanguage(requestDto.getSourceLanguage());
    rejectDuplicateLiteral(literal, null);

    LiteralTranslation saved =
        literalTranslationRepository.save(
            LiteralTranslation.builder().literal(literal).sourceLanguage(sourceLanguage).build());
    return saveTranslations(saved, literal, language, sourceLanguage, requestDto, saved.getId());
  }

  @Transactional
  public LiteralTranslationListItemDto update(
      Integer id, LiteralTranslationUpsertRequestDto requestDto) {
    LiteralTranslation literalTranslation = getLiteralTranslation(id);
    String literal = requireLiteral(requestDto.getLiteral());
    String language = requireLanguage(requestDto.getLanguage());
    Language sourceLanguage = requireSourceLanguage(requestDto.getSourceLanguage());
    rejectDuplicateLiteral(literal, id);
    validateSourceLanguageChange(literalTranslation, sourceLanguage);

    literalTranslation.setLiteral(literal);
    literalTranslation.setSourceLanguage(sourceLanguage);
    return saveTranslations(literalTranslation, literal, language, sourceLanguage, requestDto, id);
  }

  @Transactional
  public void delete(Integer id) {
    LiteralTranslation literalTranslation =
        literalTranslationRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Literal not found"));
    literalTranslationRepository.delete(literalTranslation);
  }

  private void upsertTranslationValue(
      LiteralTranslation literalTranslation, String languageCode, String rawTranslation) {
    String normalizedTranslation = normalizeOptionalText(rawTranslation);
    Optional<LiteralTranslationValue> existingValue =
        literalTranslationValueRepository.findByLiteralTranslationIdAndLanguageShortname(
            literalTranslation.getId(), languageCode);

    if (!StringUtils.hasText(normalizedTranslation)) {
      existingValue.ifPresent(literalTranslationValueRepository::delete);
      return;
    }

    Language language =
        languageRepository
            .findByShortname(languageCode)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language not found"));

    LiteralTranslationValue value = existingValue.orElseGet(LiteralTranslationValue::new);
    value.setLiteralTranslation(literalTranslation);
    value.setLanguage(language);
    value.setValue(normalizedTranslation);
    literalTranslationValueRepository.save(value);
  }

  private void upsertTranslationValues(
      LiteralTranslation literalTranslation, Map<String, String> translationsByLanguage) {
    for (Map.Entry<String, String> entry : translationsByLanguage.entrySet()) {
      String languageCode = requireLanguage(entry.getKey());
      upsertTranslationValue(literalTranslation, languageCode, entry.getValue());
    }
  }

  private void upsertRequestedTranslations(
      LiteralTranslation literalTranslation,
      LiteralTranslationUpsertRequestDto requestDto,
      String language) {
    if (requestDto.getTranslations() != null && !requestDto.getTranslations().isEmpty()) {
      upsertTranslationValues(literalTranslation, requestDto.getTranslations());
      return;
    }
    upsertTranslationValue(literalTranslation, language, requestDto.getTranslation());
  }

  private LiteralTranslationListItemDto saveTranslations(
      LiteralTranslation literalTranslation,
      String literal,
      String requestedLanguage,
      Language sourceLanguage,
      LiteralTranslationUpsertRequestDto requestDto,
      Integer id) {
    upsertRequestedTranslations(literalTranslation, requestDto, requestedLanguage);
    syncSourceTranslationValue(literalTranslation, literal);
    return toListItem(
        id,
        literal,
        resolveResponseTranslation(literal, requestDto, requestedLanguage, sourceLanguage),
        sourceLanguage.getShortname());
  }

  private void syncSourceTranslationValue(LiteralTranslation literalTranslation, String literal) {
    String sourceLanguageCode = literalTranslation.getSourceLanguage().getShortname();
    upsertTranslationValue(literalTranslation, sourceLanguageCode, literal);
  }

  private String resolveResponseTranslation(
      String literal,
      LiteralTranslationUpsertRequestDto requestDto,
      String requestedLanguage,
      Language sourceLanguage) {
    if (sourceLanguage.getShortname().equals(requestedLanguage)) {
      return literal;
    }
    return normalizeOptionalText(requestDto.getTranslation());
  }

  private LiteralTranslation getLiteralTranslation(Integer id) {
    return literalTranslationRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Literal not found"));
  }

  private void rejectDuplicateLiteral(String literal, Integer currentId) {
    Optional<LiteralTranslation> duplicate = literalTranslationRepository.findByLiteral(literal);
    if (duplicate.isPresent() && !duplicate.get().getId().equals(currentId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Literal already exists");
    }
  }

  private void validateSourceLanguageChange(
      LiteralTranslation literalTranslation, Language requestedSourceLanguage) {
    Language currentSourceLanguage = literalTranslation.getSourceLanguage();
    if (currentSourceLanguage != null
        && currentSourceLanguage.getShortname() != null
        && !currentSourceLanguage.getShortname().equals(requestedSourceLanguage.getShortname())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Source language cannot be changed");
    }
  }

  private Language requireSourceLanguage(String language) {
    return requireExistingLanguage(language, "Source language not found");
  }

  private Language requireExistingLanguage(String language, String notFoundMessage) {
    String normalized = requireLanguage(language);
    return languageRepository
        .findByShortname(normalized)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, notFoundMessage));
  }

  private String requireLanguage(String language) {
    String normalized = normalizeOptionalText(language);
    if (!StringUtils.hasText(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language is required");
    }
    return normalized;
  }

  private String requireLiteral(String literal) {
    String normalized = normalizeOptionalText(literal);
    if (!StringUtils.hasText(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Literal is required");
    }
    return normalized;
  }

  private String normalizeOptionalText(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private LiteralTranslationFilterModel parseFilter(String filter) {
    if (!StringUtils.hasText(filter)) {
      return LiteralTranslationFilterModel.empty();
    }

    try {
      JsonNode root = objectMapper.readTree(filter);
      if (!root.isObject()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter payload");
      }

      Map<String, LiteralTranslationFilterModel.ColumnFilter> columns = new LinkedHashMap<>();

      root.propertyStream()
          .iterator()
          .forEachRemaining(
              entry -> {
                JsonNode field = entry.getValue();
                List<LiteralTranslationFilterModel.Condition> parsed = parseConditions(field);
                if (parsed.isEmpty()) {
                  return;
                }
                columns.put(
                    entry.getKey(),
                    new LiteralTranslationFilterModel.ColumnFilter(
                        text(field, "operator"), parsed));
              });
      return new LiteralTranslationFilterModel(columns);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter payload", e);
    }
  }

  private List<LiteralTranslationFilterModel.Condition> parseConditions(JsonNode field) {
    List<LiteralTranslationFilterModel.Condition> parsed = new ArrayList<>();

    JsonNode conditions = field.get("conditions");
    if (conditions != null && conditions.isArray()) {
      conditions.forEach(
          node ->
              parsed.add(
                  new LiteralTranslationFilterModel.Condition(
                      text(node, "type"), text(node, FILTER))));
      return parsed;
    }

    JsonNode condition1 = field.get("condition1");
    JsonNode condition2 = field.get("condition2");
    if (condition1 != null || condition2 != null) {
      addCondition(parsed, condition1);
      addCondition(parsed, condition2);
      return parsed;
    }

    String type = text(field, "type");
    if (type != null) {
      parsed.add(new LiteralTranslationFilterModel.Condition(type, text(field, FILTER)));
    }
    return parsed;
  }

  private void addCondition(List<LiteralTranslationFilterModel.Condition> parsed, JsonNode node) {
    if (node == null || node.isNull()) {
      return;
    }
    String type = text(node, "type");
    if (type != null) {
      parsed.add(new LiteralTranslationFilterModel.Condition(type, text(node, FILTER)));
    }
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private LiteralTranslationListItemDto toListItem(
      Integer id, String literal, String translation, String sourceLanguage) {
    return new LiteralTranslationListItemDto(
        id,
        literal,
        normalizeOptionalText(translation),
        sourceLanguage,
        literalTranslationRepository.isCompleteByLiteral(literal));
  }
}
