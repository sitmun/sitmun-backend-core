package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.TreeNode;
import org.sitmun.domain.projections.TreeNodeProjection;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "tree node")
@RepositoryRestResource(collectionResourceRel = "tree-nodes", path = "tree-nodes",
  excerptProjection = TreeNodeProjection.class)
public interface TreeNodeRepository extends PagingAndSortingRepository<TreeNode, Integer> {
}