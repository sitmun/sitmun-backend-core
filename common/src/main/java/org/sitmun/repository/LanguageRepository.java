package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Language;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "language")
@RepositoryRestResource(collectionResourceRel = "languages", path = "languages")
public interface LanguageRepository
  extends PagingAndSortingRepository<Language, Integer> {
}