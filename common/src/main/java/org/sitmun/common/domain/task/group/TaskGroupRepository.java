package org.sitmun.common.domain.task.group;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "task group")
@RepositoryRestResource(collectionResourceRel = "task-groups", path = "task-groups")
public interface TaskGroupRepository extends PagingAndSortingRepository<TaskGroup, Integer> {
}