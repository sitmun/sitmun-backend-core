package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.ThematicMap;
import org.springframework.data.repository.PagingAndSortingRepository;

@Tag(name = "thematic map")
public interface ThematicMapRepository extends PagingAndSortingRepository<ThematicMap, Integer> {
}