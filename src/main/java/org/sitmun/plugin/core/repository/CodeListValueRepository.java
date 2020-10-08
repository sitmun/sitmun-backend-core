package org.sitmun.plugin.core.repository;

import org.sitmun.plugin.core.domain.CodeListValue;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "colelistvalues", path = "colelistvalues")
public interface CodeListValueRepository extends CrudRepository<CodeListValue, Integer> {

  boolean existsByCodeListNameAndValue(String codeList, String code);

}
