package org.sitmun.plugin.core.repository;

import org.sitmun.plugin.core.domain.QueryTask;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface QueryTaskRepository extends PagingAndSortingRepository<QueryTask, Integer> {
}