package org.sitmun.domain.task.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Task type Liquibase seed")
class TaskTypeSeedTest extends BaseTest {

  @Autowired TaskTypeRepository taskTypeRepository;

  @Test
  @DisplayName("seeds Template task type id 15 after Liquibase")
  void seedsTemplateTaskType() {
    TaskType template =
        taskTypeRepository
            .findById(TASK_TYPE_ID_TEMPLATE)
            .orElseThrow(() -> new AssertionError("Expected STM_TSK_TYP id 15 (template)"));

    assertThat(template.getName()).isEqualTo("template");
    assertThat(template.getTitle()).isEqualTo("Template");
    assertThat(template.getEnabled()).isTrue();
  }
}
