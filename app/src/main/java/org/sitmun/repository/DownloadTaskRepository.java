package org.sitmun.repository;

import org.sitmun.domain.DownloadTask;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface DownloadTaskRepository extends PagingAndSortingRepository<DownloadTask, Integer> {
}