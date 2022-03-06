package org.sitmun.common.domain.task;

import com.querydsl.core.types.dsl.SimpleExpression;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;

import java.util.Set;

@Tag(name = "task")
@RepositoryRestResource(collectionResourceRel = "tasks", path = "tasks")
public interface TaskRepository extends PagingAndSortingRepository<Task, Integer>,
  QuerydslPredicateExecutor<Task>,
  QuerydslBinderCustomizer<QTask> {

  Iterable<Task> findAllByTypeId(@NonNull Integer typeId);

  @Query("select task from Task task, Application app, Role role where app.id = :applicationId and role member of app.availableRoles and role member of task.roles")
  Set<Task> available(@Param("applicationId") @NonNull Integer applicationId);

  @Override
  default void customize(QuerydslBindings bindings, QTask root) {
    //noinspection NullableProblems
    bindings.bind(root.type.id).first(SimpleExpression::eq);
  }
}