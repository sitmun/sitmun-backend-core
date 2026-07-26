package org.sitmun.domain.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("MIA E2E Liquibase seed")
class MiaE2eSeedTest extends BaseTest {

  @Autowired private TaskRepository taskRepository;

  @Test
  @DisplayName("seeds MIA parent task 42 with childTaskOrderIds containing 38")
  void seedsMiaParentWithQueryChild() {
    Task parent =
        taskRepository
            .findById(42)
            .orElseThrow(() -> new AssertionError("Expected seeded STM_TASK id 42"));
    assertThat(parent.getName()).contains("MIA");
    Map<String, Object> properties = parent.getProperties();
    assertThat(properties).isNotNull();
    assertThat(properties.get("childTaskOrderIds")).asList().contains(38);
  }
}
