package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Comment;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "comment")
@RepositoryRestResource(collectionResourceRel = "comments", path = "comments")
public interface CommentRepository extends PagingAndSortingRepository<Comment, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Comment> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull Comment entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.Comment','administration') or hasPermission(#entityId, 'org.sitmun.domain.Comment', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<Comment> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.Comment','administration') or hasPermission(#entityId, 'org.sitmun.domain.Comment', 'read')")
  @NonNull
  Optional<Comment> findById(@P("entityId") @NonNull Integer entityId);

}
