package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
@RequiredArgsConstructor
public class LanguageEventHandler {

  private final LiteralTranslationRepository literalTranslationRepository;
  private final LiteralTranslationValueRepository literalTranslationValueRepository;

  @HandleBeforeDelete
  @Transactional
  public void handleLanguageDelete(@NotNull Language language) {
    Integer languageId = language.getId();

    List<Integer> sourceLiteralIds =
        literalTranslationRepository.findIdsBySourceLanguageId(languageId);
    if (!sourceLiteralIds.isEmpty()) {
      literalTranslationValueRepository.deleteAllByLiteralTranslationIdIn(sourceLiteralIds);
      literalTranslationRepository.deleteAllBySourceLanguageId(languageId);
    }

    literalTranslationValueRepository.deleteAllByLanguageId(languageId);
  }
}
