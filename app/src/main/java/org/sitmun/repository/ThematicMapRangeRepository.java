package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.ThematicMapRange;
import org.sitmun.domain.ThematicMapRangeId;
import org.springframework.data.repository.PagingAndSortingRepository;

@Tag(name = "thematic map range")
public interface ThematicMapRangeRepository
  extends PagingAndSortingRepository<ThematicMapRange, ThematicMapRangeId> {
}