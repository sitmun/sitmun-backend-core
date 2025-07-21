package org.sitmun.infrastructure.persistence.type.codelist;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "code list")
@RepositoryRestResource(collectionResourceRel = "codelist-values", path = "codelist-values")
public interface CodeListValueRepository
    extends JpaRepository<CodeListValue, Integer>, QuerydslPredicateExecutor<CodeListValue> {

  boolean existsByCodeListNameAndValue(String codeList, String code);

  boolean deleteByCodeListName(String name);

  @Query("select distinct codeListName from CodeListValue")
  Iterable<String> findDistinctCodeListName();

  Iterable<CodeListValue> findAllByCodeListName(String codeListaName);
}
