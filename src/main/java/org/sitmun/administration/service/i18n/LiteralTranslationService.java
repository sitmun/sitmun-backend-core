package org.sitmun.administration.service.i18n;

import lombok.RequiredArgsConstructor;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LiteralTranslationService implements LiteralTranslationResolver {

  private final LiteralTranslationRepository literalTranslationRepository;
  private final LiteralTranslationValueRepository literalTranslationValueRepository;

  @Override
  @Transactional(readOnly = true)
  public String resolve(String literal, String language) {
    if (literal == null || !StringUtils.hasText(language)) {
      return literal;
    }

    return literalTranslationRepository
        .findByLiteral(literal)
        .flatMap(
            literalTranslation ->
                literalTranslationValueRepository.findValueByLiteralIdAndLanguage(
                    literalTranslation.getId(), language))
        .filter(StringUtils::hasText)
        .orElse(literal);
  }
}
