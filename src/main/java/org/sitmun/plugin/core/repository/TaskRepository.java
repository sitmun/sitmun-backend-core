package org.sitmun.plugin.core.repository;

import com.querydsl.core.types.dsl.SimpleExpression;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.QTask;
import org.sitmun.plugin.core.domain.Task;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "task")
@RepositoryRestResource(collectionResourceRel = "tasks", path = "tasks")
public interface TaskRepository extends PagingAndSortingRepository<Task, Integer>,
  QuerydslPredicateExecutor<Task>,
  QuerydslBinderCustomizer<QTask> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Task> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull Task entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Task', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostAuthorize("hasPermission(returnObject, 'read')")
  @NonNull
  Iterable<Task> findAll();

  @Override
  @PostAuthorize("hasPermission(#entityId, 'administration') or hasPermission(returnObject, 'read')")
  @NonNull
  Optional<Task> findById(@P("entityId") @NonNull Integer entityId);

  Iterable<Task> findAllByTypeId(@NonNull Integer typeId);

  @Query("select distinct task from Task task, Application app, Role role where app.id = :applicationId and role member of app.availableRoles and role member of task.roles")
  Iterable<Task> available(@P("applicationId") @NonNull Integer applicationId);

  @Override
  default void customize(QuerydslBindings bindings, QTask root) {
    //noinspection NullableProblems
    bindings.bind(root.type.id).first(SimpleExpression::eq);
  }
}