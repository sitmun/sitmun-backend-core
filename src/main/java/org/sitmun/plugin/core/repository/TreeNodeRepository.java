package org.sitmun.plugin.core.repository;


import java.math.BigInteger;
import java.util.List;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.TreeNode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "tree-nodes", path = "tree-nodes"/*, excerptProjection = TreeNodeProjection.class*/)
public interface TreeNodeRepository extends CrudRepository<TreeNode, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  TreeNode save(@P("entity") TreeNode entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") TreeNode entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TreeNode', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(#entity, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<TreeNode> findAll();

  @RestResource(exported = false)
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select treeNode.cartography from TreeNode treeNode where treeNode.id =:id")
  List<Cartography> findCartography(@Param("id") BigInteger id);
	
	/*
	@Query("select treeNode from TreeNode treeNode left join fetch treeNode.cartography where treeNode.id =:id")    
	TreeNode findOneWithEagerRelationships(long id);
	*/


}