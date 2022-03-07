package org.sitmun.legacy.domain.task.thematic.range;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;

@Tag(name = "thematic map range")
public interface ThematicMapRangeRepository
  extends PagingAndSortingRepository<ThematicMapRange, ThematicMapRangeId> {
}