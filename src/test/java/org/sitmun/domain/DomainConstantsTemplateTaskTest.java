package org.sitmun.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DomainConstantsTemplateTaskTest {

  @Test
  void templateTaskTypeIdIsFifteen() {
    assertEquals(15, DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE);
  }

  @Test
  void templateRelationTypesAreStable() {
    assertEquals("template-task", DomainConstants.Tasks.RELATION_TYPE_TEMPLATE_TASK);
    assertEquals("template-nested", DomainConstants.Tasks.RELATION_TYPE_TEMPLATE_NESTED);
  }
}
