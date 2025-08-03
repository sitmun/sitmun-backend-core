package org.sitmun.infrastructure.persistence.type.i18n;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "translation")
@RepositoryRestResource(collectionResourceRel = "translations", path = "translations")
public interface TranslationRepository extends JpaRepository<Translation, Integer> {

  @Query(
      "select tr from Translation tr where tr.element = :entityId and tr.column like :entity% and tr.language.shortname = :locale")
  List<Translation> findTranslation(
      @Param("entityId") Integer entityId,
      @Param("entity") String coordinates,
      @Param("locale") String locale);
}
