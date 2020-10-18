package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.CodeListValue;
import org.sitmun.plugin.core.domain.Comment;
import org.sitmun.plugin.core.domain.Connection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "comment")
@RepositoryRestResource(collectionResourceRel = "comments", path = "comments")
public interface CommentRepository extends CrudRepository<Comment, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends Comment> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Comment entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Comment','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Connection', 'delete')")
  void deleteById(@P("entityId") Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Comment> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Comment','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Connection', 'read')")
  Optional<Comment> findById(@P("entityId") Integer entityId);

}
