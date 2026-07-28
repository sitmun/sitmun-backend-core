package org.sitmun.infrastructure.persistence.type.i18n;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "language")
@RepositoryRestResource(collectionResourceRel = "languages", path = "languages")
public interface LanguageRepository extends JpaRepository<Language, Integer> {

  List<Language> findAllByOrderByOrderAscIdAsc();

  Optional<Language> findFirstByDefaultLanguageTrue();

  @Modifying
  @Query(
      "update Language language set language.defaultLanguage = false where language.defaultLanguage = true")
  int clearAllDefaultFlags();

  /**
   * Default collection order: {@code order} ascending (nulls last), then {@code shortname}. ORDER
   * BY in the query is intentional so clients need not pass {@code ?sort=}.
   */
  @Override
  @Query(
      value =
          """
          select language
          from Language language
          order by case when language.order is null then 1 else 0 end,
                   language.order asc,
                   language.shortname asc
          """,
      countQuery = "select count(language) from Language language")
  Page<Language> findAll(Pageable pageable);

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select language
      from Language language
      where lower(language.shortname) like lower(concat('%', :q, '%'))
      or lower(language.name) like lower(concat('%', :q, '%'))
      order by case when language.order is null then 1 else 0 end,
               language.order asc,
               language.shortname asc
      """)
  Page<Language> findByContent(@Param("q") String q, Pageable pageable);

  /**
   * Enabled languages only, same default order as {@link #findAll(Pageable)}.
   *
   * <p>Exposed as {@code GET /api/languages/search/enabled}.
   */
  @RestResource(path = "enabled", rel = "enabled")
  @Query(
      value =
          """
          select language
          from Language language
          where language.enabled = true
          order by case when language.order is null then 1 else 0 end,
                   language.order asc,
                   language.shortname asc
          """,
      countQuery = "select count(language) from Language language where language.enabled = true")
  Page<Language> findByEnabledTrue(Pageable pageable);

  /**
   * Find language by BCP-47 shortname.
   *
   * @param shortname BCP-47 language tag
   * @return Language if found
   */
  Optional<Language> findByShortname(String shortname);

  @RestResource(exported = false)
  List<Language> findByShortnameIn(Collection<String> shortnames);
}
