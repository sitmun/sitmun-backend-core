package org.sitmun.infrastructure.persistence.type.codelist;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "code list")
@RepositoryRestResource(collectionResourceRel = "codelist-values", path = "codelist-values")
public interface CodeListValueRepository
    extends JpaRepository<CodeListValue, Integer>, QuerydslPredicateExecutor<CodeListValue> {

  boolean existsByCodeListNameAndValue(String codeList, String code);

  boolean deleteByCodeListName(String name);

  @Query("select distinct codeListName from CodeListValue")
  Iterable<String> findDistinctCodeListName();

  Iterable<CodeListValue> findAllByCodeListName(String codeListaName);

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select codeListValue
      from CodeListValue codeListValue
      where lower(codeListValue.value) like lower(concat('%', :q, '%'))
      or lower(codeListValue.description) like lower(concat('%', :q, '%'))
      or lower(codeListValue.codeListName) like lower(concat('%', :q, '%'))
      """)
  Page<CodeListValue> findByContent(@Param("q") String q, Pageable pageable);
}
