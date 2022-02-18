package org.sitmun.plugin.core.repository;

import org.sitmun.plugin.core.domain.DownloadTask;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface DownloadTaskRepository extends PagingAndSortingRepository<DownloadTask, Integer> {
}