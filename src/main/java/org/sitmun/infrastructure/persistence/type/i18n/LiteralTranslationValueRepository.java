package org.sitmun.infrastructure.persistence.type.i18n;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(exported = false)
public interface LiteralTranslationValueRepository
    extends JpaRepository<LiteralTranslationValue, Integer> {

  @RestResource(exported = false)
  Optional<LiteralTranslationValue> findByLiteralTranslationIdAndLanguageShortname(
      Integer literalTranslationId, String shortname);

  @RestResource(exported = false)
  @Query(
      """
      select value.value
      from LiteralTranslationValue value
      where value.literalTranslation.id = :literalId
      and value.language.shortname = :language
      """)
  Optional<String> findValueByLiteralIdAndLanguage(
      @Param("literalId") Integer literalId, @Param("language") String language);

  @RestResource(exported = false)
  @Query(
      """
      SELECT v FROM LiteralTranslationValue v
      JOIN FETCH v.literalTranslation
      WHERE v.language.id = :languageId
      AND v.literalTranslation.id IN :literalIds
      """)
  List<LiteralTranslationValue> findByLanguageIdAndLiteralTranslationIdIn(
      @Param("languageId") Integer languageId, @Param("literalIds") Collection<Integer> literalIds);

  @RestResource(exported = false)
  @Modifying
  @Query("delete from LiteralTranslationValue value where value.language.id = :languageId")
  void deleteAllByLanguageId(@Param("languageId") Integer languageId);

  @RestResource(exported = false)
  @Modifying
  @Query(
      "delete from LiteralTranslationValue value where value.literalTranslation.id in :literalIds")
  void deleteAllByLiteralTranslationIdIn(@Param("literalIds") Collection<Integer> literalIds);

  long countByLanguage_Shortname(String shortname);
}
