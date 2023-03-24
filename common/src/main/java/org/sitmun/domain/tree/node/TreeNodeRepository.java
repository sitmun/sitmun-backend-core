package org.sitmun.domain.tree.node;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "tree node")
@RepositoryRestResource(collectionResourceRel = "tree-nodes", path = "tree-nodes",
  excerptProjection = TreeNodeProjection.class)
public interface TreeNodeRepository extends PagingAndSortingRepository<TreeNode, Integer> {
}