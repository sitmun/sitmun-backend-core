package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import org.sitmun.plugin.core.domain.Log;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "log")
@RepositoryRestResource(collectionResourceRel = "logs", path = "logs")
public interface LogRepository extends PagingAndSortingRepository<Log, BigInteger> {
}