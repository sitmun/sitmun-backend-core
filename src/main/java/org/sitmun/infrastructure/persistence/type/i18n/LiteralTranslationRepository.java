package org.sitmun.infrastructure.persistence.type.i18n;

import java.util.Optional;
import org.sitmun.administration.controller.dto.LiteralTranslationListItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(exported = false)
public interface LiteralTranslationRepository extends JpaRepository<LiteralTranslation, Integer> {

  @RestResource(exported = false)
  @Query("SELECT lt FROM LiteralTranslation lt WHERE lt.literal = :literal")
  Optional<LiteralTranslation> findByLiteral(@Param("literal") String literal);

  @RestResource(exported = false)
  @Query(
      value =
          """
          select new org.sitmun.administration.controller.dto.LiteralTranslationListItemDto(
            literalTranslation.id,
            literalTranslation.literal,
            case
              when literalTranslation.sourceLanguage.shortname = :language then literalTranslation.literal
              else literalValue.value
            end,
            literalTranslation.sourceLanguage.shortname,
            (select count(lang) from Language lang) = (select count(lv) from LiteralTranslationValue lv where lv.literalTranslation = literalTranslation)
          )
          from LiteralTranslation literalTranslation
          left join LiteralTranslationValue literalValue
            on literalValue.literalTranslation = literalTranslation
            and literalValue.language.shortname = :language
          """,
      countQuery = "select count(literalTranslation) from LiteralTranslation literalTranslation")
  Page<LiteralTranslationListItemDto> findPageByLanguage(
      @Param("language") String language, Pageable pageable);

  @Query(
      value =
          """
      SELECT (select count(lang) from Language lang) = (select count(lv) from LiteralTranslationValue lv where lv.literalTranslation.literal = :literal)
      """)
  boolean isCompleteByLiteral(@Param("literal") String literal);

  @Query(value = "SELECT count(id) FROM LiteralTranslation")
  long countTotalTranslations();
}
