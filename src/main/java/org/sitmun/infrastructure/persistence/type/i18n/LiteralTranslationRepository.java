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
public interface LiteralTranslationRepository
    extends JpaRepository<LiteralTranslation, Integer>, LiteralTranslationQuerydslRepository {

  @RestResource(exported = false)
  @Query("SELECT lt FROM LiteralTranslation lt WHERE lt.literal = :literal")
  Optional<LiteralTranslation> findByLiteral(@Param("literal") String literal);

  @RestResource(exported = false)
  @Query(
      """
      SELECT lt FROM LiteralTranslation lt
      JOIN FETCH lt.sourceLanguage
      WHERE lt.literal IN :literals
      """)
  List<LiteralTranslation> findByLiteralIn(@Param("literals") Collection<String> literals);

  @RestResource(exported = false)
  @Query(
      """
      SELECT lt FROM LiteralTranslation lt
      JOIN FETCH lt.sourceLanguage
      ORDER BY lt.literal ASC
      """)
  List<LiteralTranslation> findAllWithSourceLanguageOrderByLiteral();

  @RestResource(exported = false)
  @Query(
      """
      SELECT lt FROM LiteralTranslation lt
      JOIN FETCH lt.sourceLanguage
      WHERE lt.id IN :ids
      ORDER BY lt.literal ASC
      """)
  List<LiteralTranslation> findByIdInWithSourceLanguageOrderByLiteral(
      @Param("ids") Collection<Integer> ids);

  @RestResource(exported = false)
  @Query(
      "select literalTranslation.id from LiteralTranslation literalTranslation where literalTranslation.sourceLanguage.id = :languageId")
  List<Integer> findIdsBySourceLanguageId(@Param("languageId") Integer languageId);

  @RestResource(exported = false)
  @Modifying
  @Query(
      "delete from LiteralTranslation literalTranslation where literalTranslation.sourceLanguage.id = :languageId")
  void deleteAllBySourceLanguageId(@Param("languageId") Integer languageId);

  @RestResource(exported = false)
  @Query(
      value =
          """
        SELECT (select count(lang) from Language lang) =
                (select count(lv) from LiteralTranslationValue lv
                 where lv.literalTranslation.literal = :literal)
        """)
  boolean isCompleteByLiteral(@Param("literal") String literal);

  @Query(value = "SELECT count(id) FROM LiteralTranslation")
  long countTotalTranslations();
}
