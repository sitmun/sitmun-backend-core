package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.CodeListValue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "code list")
@RepositoryRestResource(collectionResourceRel = "codelist-values", path = "codelist-values")
public interface CodeListValueRepository extends
    PagingAndSortingRepository<CodeListValue, Integer> {

  boolean existsByCodeListNameAndValue(String codeList, String code);

  @Query("select distinct codeListName from CodeListValue")
  Iterable<String> findDistinctCodeListName();

  @Query("select value from CodeListValue where codeListName = :codeListName")
  Iterable<String> findValuesByCodeListName(String codeListName);
}
