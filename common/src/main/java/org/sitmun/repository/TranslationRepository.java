package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Translation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@Tag(name = "translation")
@RepositoryRestResource(collectionResourceRel = "translations", path = "translations")
public interface TranslationRepository
  extends PagingAndSortingRepository<Translation, Integer> {

  @Query("select tr from Translation tr where tr.element = :entityId and tr.language.shortname = :locale")
  List<Translation> findByEntityIdAndLocale(
    @Param("entityId") Integer entityId,
    @Param("locale") String locale);
}