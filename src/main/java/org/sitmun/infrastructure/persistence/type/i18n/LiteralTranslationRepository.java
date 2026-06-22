package org.sitmun.infrastructure.persistence.type.i18n;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
      value =
          """
        SELECT (select count(lang) from Language lang) =
                (select count(lv) from LiteralTranslationValue lv where lv.literalTranslation.literal = :literal)
        """)
  boolean isCompleteByLiteral(@Param("literal") String literal);

  @Query(value = "SELECT count(id) FROM LiteralTranslation")
  long countTotalTranslations();
}
