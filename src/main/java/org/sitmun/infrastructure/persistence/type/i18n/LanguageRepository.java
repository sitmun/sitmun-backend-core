package org.sitmun.infrastructure.persistence.type.i18n;

import io.swagger.v3.oas.annotations.tags.Tag;
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

  Optional<Language> findByShortname(String shortname);

  @Modifying
  @Query(
      "update Language language set language.defaultLanguage = false where language.defaultLanguage = true")
  int clearAllDefaultFlags();

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select language
      from Language language
      where lower(language.shortname) like lower(concat('%', :q, '%'))
      or lower(language.name) like lower(concat('%', :q, '%'))
      """)
  Page<Language> findByContent(@Param("q") String q, Pageable pageable);
}
