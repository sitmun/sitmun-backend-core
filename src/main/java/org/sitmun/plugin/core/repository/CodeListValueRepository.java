package org.sitmun.plugin.core.repository;

import org.sitmun.plugin.core.domain.CodeListValue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "colelistvalues", path = "colelistvalues")
public interface CodeListValueRepository extends CrudRepository<CodeListValue, Integer> {

  boolean existsByCodeListNameAndValue(String codeList, String code);

  @Query("select distinct codeListName from CodeListValue")
  Iterable<String> findDistinctCodeListName();

  @Query("select value from CodeListValue where codeListName = :codeListName")
  Iterable<String> findValuesByCodeListName(String codeListName);
}
