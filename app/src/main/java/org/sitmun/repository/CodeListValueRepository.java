package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.CodeListValue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "code list")
@RepositoryRestResource(collectionResourceRel = "codelist-values", path = "codelist-values")
public interface CodeListValueRepository extends
  PagingAndSortingRepository<CodeListValue, Integer>,
  QuerydslPredicateExecutor<CodeListValue> {

  boolean existsByCodeListNameAndValue(String codeList, String code);

  boolean deleteByCodeListName(String name);

  @Query("select distinct codeListName from CodeListValue")
  Iterable<String> findDistinctCodeListName();

  Iterable<CodeListValue> findAllByCodeListName(String codeListaName);
}
