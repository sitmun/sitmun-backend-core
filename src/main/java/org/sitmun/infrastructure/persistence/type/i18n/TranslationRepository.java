package org.sitmun.infrastructure.persistence.type.i18n;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "translation")
@RepositoryRestResource(
    collectionResourceRel = "translations",
    path = "translations",
    excerptProjection = TranslationProjection.class)
public interface TranslationRepository extends JpaRepository<Translation, Integer> {

  @Query(
      "select tr from Translation tr where tr.element = :entityId and tr.column like :entity% and tr.language.shortname = :locale")
  List<Translation> findTranslation(
      @Param("entityId") Integer entityId,
      @Param("entity") String coordinates,
      @Param("locale") String locale);

  /**
   * Find all translations for a specific entity instance across all languages. This is more
   * efficient than fetching all translations and filtering client-side. Fetches language in the
   * same query to avoid lazy-load and ResultSet closed issues during serialization.
   *
   * @param element The entity ID
   * @param column The column prefix (e.g., "Application", "Language")
   * @return List of translations for the specified entity
   */
  @RestResource(path = "byElement", rel = "byElement")
  @EntityGraph(attributePaths = "language")
  List<Translation> findByElementAndColumnStartingWith(
      @Param("element") Integer element, @Param("column") String column);

  /**
   * Bulk-load translation rows for a locale without hydrating entities. Used to populate
   * request-scoped cache and avoid nested queries during @PostLoad.
   */
  @Query(
      """
  select new org.sitmun.infrastructure.persistence.type.i18n.TranslationRow(tr.element, tr.column, tr.translation)
  from Translation tr
  where tr.language.shortname = :locale
  """)
  List<TranslationRow> findAllByLocaleRows(@Param("locale") String locale);
}
