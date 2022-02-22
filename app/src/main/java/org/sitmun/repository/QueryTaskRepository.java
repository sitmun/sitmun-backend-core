package org.sitmun.repository;

import org.sitmun.domain.QueryTask;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface QueryTaskRepository extends PagingAndSortingRepository<QueryTask, Integer> {
}