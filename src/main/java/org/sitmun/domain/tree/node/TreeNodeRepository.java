package org.sitmun.domain.tree.node;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.tree.Tree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "tree node")
@RepositoryRestResource(
    collectionResourceRel = "tree-nodes",
    path = "tree-nodes",
    excerptProjection = TreeNodeProjection.class)
public interface TreeNodeRepository extends JpaRepository<TreeNode, Integer> {

  @Query("select node from TreeNode node where " + "node.tree in ?1")
  List<TreeNode> findByTrees(List<Tree> trees);

  @Query(
      "SELECT CASE WHEN COUNT(tn) > 0 THEN true ELSE false END "
          + "FROM TreeNode tn WHERE tn.cartography.id = ?1 AND tn.style = ?2")
  boolean existsByCartographyIdAndStyle(Integer cartographyId, String style);

  @Query(
      "SELECT CASE WHEN COUNT(tn) > 0 THEN true ELSE false END "
          + "FROM TreeNode tn WHERE tn.cartography.id = ?1")
  boolean existsByCartographyId(Integer cartographyId);
}
