package org.sitmun.domain.task;

import com.querydsl.core.types.dsl.SimpleExpression;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.role.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Set;

@Tag(name = "task")
@RepositoryRestResource(collectionResourceRel = "tasks", path = "tasks")
public interface TaskRepository extends PagingAndSortingRepository<Task, Integer>,
  QuerydslPredicateExecutor<Task>,
  QuerydslBinderCustomizer<QTask> {

  Iterable<Task> findAllByTypeId(@NonNull Integer typeId);

  @Query("select task from Task task, Application app, Role role where app.id = :applicationId and role member of app.availableRoles and role member of task.roles")
  Set<Task> available(@Param("applicationId") @NonNull Integer applicationId);

  @RestResource(exported = false)
  @Query("SELECT tsk FROM Task tsk WHERE tsk.id in " +      // Because we cannot use DISTINCT in Oracle
    "(SELECT tsk.id FROM Task tsk, TaskAvailability tav, Role rol WHERE " +
    "rol member tsk.roles AND rol in ?1 " +                 // Task is linked to any of the roles
    "AND tsk.id = tav.task.id AND tav.territory.id = ?2)")  // AND the task is available to the territory
  List<Task> findByRolesAndTerritory(List<Role> roles, Integer territoryId);

  @Override
  default void customize(QuerydslBindings querydslBindings, QTask root) {
    querydslBindings.bind(root.type.id).first(SimpleExpression::eq);
  }
}