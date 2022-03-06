package org.sitmun.common.domain.tree;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "tree")
@RepositoryRestResource(collectionResourceRel = "trees", path = "trees"/*, excerptProjection = TreeProjection.class*/)
public interface TreeRepository extends PagingAndSortingRepository<Tree, Integer> {
  @Query("select tree from Tree tree left join fetch tree.allNodes where tree.id =:id")
  Tree findOneWithEagerRelationships(@Param("id") long id);
}