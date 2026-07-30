package org.sitmun.domain.task.relation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.database.DatabaseConnectionRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.group.TaskGroup;
import org.sitmun.domain.task.group.TaskGroupRepository;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.task.type.TaskTypeRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@DataJpaTest
@DisplayName("TaskRelation Repository JPA Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRelationRepositoryTest {

  @Autowired private TaskRelationRepository taskRelationRepository;
  @Autowired private TaskRepository taskRepository;
  @Autowired private TaskGroupRepository taskGroupRepository;
  @Autowired private TaskTypeRepository taskTypeRepository;
  @Autowired private DatabaseConnectionRepository databaseConnectionRepository;
  @Autowired private TestEntityManager entityManager;

  @Test
  @DisplayName("findByTaskId initializes EAGER related-task to-ones without nested ResultSet use")
  void findByTaskIdInitializesRelatedTaskEagerToOnes() {
    TaskGroup group = taskGroupRepository.save(TaskGroup.builder().name("rel-fetch-group").build());
    TaskType sqlType =
        taskTypeRepository
            .findById(DomainConstants.Tasks.TASK_TYPE_ID_QUERY)
            .orElseThrow(() -> new IllegalStateException("Seed TaskType QUERY (5) required"));
    TaskType templateType =
        taskTypeRepository
            .findById(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
            .orElseThrow(() -> new IllegalStateException("Seed TaskType TEMPLATE (15) required"));
    DatabaseConnection connection =
        databaseConnectionRepository.save(
            DatabaseConnection.builder()
                .name("rel-fetch-conn")
                .driver("org.h2.Driver")
                .url("jdbc:h2:mem:rel-fetch")
                .user("sa")
                .password("")
                .build());

    Task parent =
        taskRepository.save(
            Task.builder()
                .name("rel-fetch-parent")
                .type(templateType)
                .group(group)
                .properties(
                    Map.of(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML, "<p>{{c.html}}</p>"))
                .build());
    Task sqlChild =
        taskRepository.save(
            Task.builder()
                .name("rel-fetch-sql")
                .type(sqlType)
                .group(group)
                .connection(connection)
                .properties(
                    Map.of(
                        DomainConstants.Tasks.PROPERTY_SCOPE,
                        DomainConstants.Tasks.SCOPE_SQL_QUERY,
                        DomainConstants.Tasks.PROPERTY_COMMAND,
                        "SELECT 1"))
                .build());
    taskRelationRepository.save(
        TaskRelation.builder()
            .task(parent)
            .relatedTask(sqlChild)
            .relationType(DomainConstants.Tasks.RELATION_TYPE_TEMPLATE_TASK)
            .referenceAlias("c")
            .build());

    entityManager.flush();
    entityManager.clear();

    List<TaskRelation> relations = taskRelationRepository.findByTaskId(parent.getId());

    assertThat(relations).hasSize(1);
    Task related = relations.get(0).getRelatedTask();
    assertThat(Hibernate.isInitialized(related)).isTrue();
    assertThat(Hibernate.isInitialized(related.getType())).isTrue();
    assertThat(Hibernate.isInitialized(related.getGroup())).isTrue();
    assertThat(Hibernate.isInitialized(related.getConnection())).isTrue();
    assertThat(related.getConnection().getId()).isEqualTo(connection.getId());
    assertThat(related.getConnection().getDriver()).isEqualTo("org.h2.Driver");
  }

  @TestConfiguration
  @Import(I18nTestConfiguration.class)
  static class Configuration {}
}
